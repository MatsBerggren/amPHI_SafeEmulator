package com.dedalus.amphi_integration.service.impl;

import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.dedalus.amphi_integration.classes.DateFix;
import com.dedalus.amphi_integration.classes.LocalDateTimeDeserializer;
import com.dedalus.amphi_integration.dto.EvamVehicleStateRequestDTO;
import com.dedalus.amphi_integration.model.OperationDistance;
import com.dedalus.amphi_integration.model.amphi.StateEntry;
import com.dedalus.amphi_integration.model.evam.Operation;
import com.dedalus.amphi_integration.model.evam.OperationState;
import com.dedalus.amphi_integration.model.evam.VehicleState;
import com.dedalus.amphi_integration.repository.AmphiStateEntryRepository;
import com.dedalus.amphi_integration.repository.EvamOperationRepository;
import com.dedalus.amphi_integration.repository.EvamVehicleStateRepository;
import com.dedalus.amphi_integration.repository.EvamVehicleStatusRepository;
import com.dedalus.amphi_integration.repository.OperationDistanceRepository;
import com.dedalus.amphi_integration.service.AmphiStateEntryService;
import com.dedalus.amphi_integration.service.EvamVehicleStateService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

@Service
public class EvamVehicleStateServiceImpl implements EvamVehicleStateService {

    @Autowired
    private AmphiStateEntryRepository amphiStateEntryRepository;

    @Autowired
    private EvamVehicleStateRepository evamVehicleStateRepository;

    @Autowired
    private EvamVehicleStatusRepository evamVehicleStatusRepository;

    @Autowired
    private OperationDistanceRepository operationDistanceRepository;

    @Autowired
    private AmphiStateEntryService amphiStateEntryService;

    @Autowired
    private EvamOperationRepository evamOperationRepository;

    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeDeserializer())
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    @Override
    public VehicleState updateVehicleState(EvamVehicleStateRequestDTO evamVehicleStateRequestDTO) {
        VehicleState vehicleState = gson.fromJson(evamVehicleStateRequestDTO.getVehicleState(), VehicleState.class);

        System.out.println("START *****************************************************************************");

        String lastVehicleStatusId = getPreviousStateId();
        System.out.println("Previous State ID: " + lastVehicleStatusId);

        String vehicleStatusId = getVehicleStatusId(vehicleState);
        System.out.println("Actual State ID: " + vehicleStatusId);

        double distance = calculateDistance(vehicleState);

        if (distance > 0) {
            updateOperationDistance(vehicleState, distance, lastVehicleStatusId, vehicleStatusId);
        }

        if (!lastVehicleStatusId.equals(vehicleStatusId)) {
            
            Optional<OperationDistance> latestOperationDistanceForPreviousState = operationDistanceRepository
                    .findFirstByOperationIDAndStateIDOrderByTimestampDesc(vehicleState.getActiveCaseFullId(), lastVehicleStatusId);
            double stateEntryDistance = latestOperationDistanceForPreviousState.map(OperationDistance::getStateEntryDistance).orElse(0.0);
            System.out.println(stateEntryDistance + vehicleState.getActiveCaseFullId() + lastVehicleStatusId);
            System.out.println(latestOperationDistanceForPreviousState.toString());
            StateEntry stateEntry = StateEntry.builder()
                    .id(vehicleStatusId)
                    .fromId(lastVehicleStatusId)
                    .distance((int) stateEntryDistance)
                    .time(DateFix.dateFixLong(vehicleState.getTimestamp()))
                    .build();
            amphiStateEntryService.updateStateEntry(stateEntry);
            
            updateOperationState(vehicleStatusId);
        }

        evamVehicleStateRepository.deleteById("1");
        vehicleState.setId("1");

        System.out.println("END *****************************************************************************");

        return evamVehicleStateRepository.save(vehicleState);
    }

    private String getPreviousStateId() {
        Optional<StateEntry> previousStateEntry = amphiStateEntryRepository.findFirstByOrderByTimeDesc();
        String previousStateId = previousStateEntry.map(StateEntry::getId).orElse("0");
        return previousStateId;
    }

    /**
     * Retrieves the vehicle status ID from the provided VehicleState.
     * If the vehicle status is not found, returns "0".
     *
     * @param vehicleState the VehicleState object containing vehicle status information
     * @return the ID of the vehicle status, or "0" if not found
     */
    private String getVehicleStatusId(VehicleState vehicleState) {
        return Optional.ofNullable(vehicleState.getVehicleStatus())
                        .map(status -> evamVehicleStatusRepository.findByName(status.getName()))
                        .map(status -> status.getId())
                        .orElse("0");
    }

    private double calculateDistance(VehicleState vehicleState) {
        Optional<VehicleState> latestVehicleState = evamVehicleStateRepository.findFirstByOrderByTimestampDesc();

        if (latestVehicleState.isPresent() && vehicleState.getVehicleLocation() != null && latestVehicleState.get().getVehicleLocation() != null) {
            return CalculateDistance(
                    latestVehicleState.get().getVehicleLocation().getLatitude(),
                    vehicleState.getVehicleLocation().getLatitude(),
                    latestVehicleState.get().getVehicleLocation().getLongitude(),
                    vehicleState.getVehicleLocation().getLongitude(),
                    0.0,
                    0.0);
        }
        return 0;
    }
    
    private void updateOperationDistance(VehicleState vehicleState, double distance, String lastVehicleStatusId, String vehicleStatusId) {
        Optional<OperationDistance> latestOperationDistance = operationDistanceRepository.findFirstByOrderByTimestampDesc();

        OperationDistance operationDistance = latestOperationDistance.map(od -> {
            double totalAssignmentDistance = od.getAssignmentDistance() == null ? distance : od.getAssignmentDistance() + distance;
            double totalStateEntryDistance = od.getStateEntryDistance() == null ? distance : od.getStateEntryDistance() + distance;

            if (!od.getOperationID().equals(vehicleState.getActiveCaseFullId())) {
                totalAssignmentDistance = distance;
                totalStateEntryDistance = distance;
            }

            if (!lastVehicleStatusId.equals(vehicleStatusId)) {
                totalStateEntryDistance = distance;
            }

            return OperationDistance.builder()
                    .timestamp(LocalDateTime.now())
                    .operationID(vehicleState.getActiveCaseFullId())
                    .distance(distance)
                    .assignmentDistance(totalAssignmentDistance)
                    .stateEntryDistance(totalStateEntryDistance)
                    .stateID(vehicleStatusId)
                    .location(vehicleState.getVehicleLocation())
                    .build();
        }).orElseGet(() -> {
            return OperationDistance.builder()
                    .timestamp(LocalDateTime.now())
                    .operationID(vehicleState.getActiveCaseFullId())
                    .distance(distance)
                    .assignmentDistance(distance)
                    .stateEntryDistance(distance)
                    .stateID(vehicleStatusId)
                    .location(vehicleState.getVehicleLocation())
                    .build();
        });
        operationDistanceRepository.save(operationDistance);
    }

    private void updateOperationState(String vehicleStatusId) {
        Optional<Operation> existingOperation = evamOperationRepository.findById("1");
        existingOperation.ifPresent(operation -> {
            int vehicleStatusIdInt = Integer.parseInt(vehicleStatusId);
            operation.setOperationState(vehicleStatusIdInt >= 5 ? OperationState.AVAILABLE : OperationState.ACTIVE);
            evamOperationRepository.save(operation);
        });
    }

    /**
     * Calculate distance between two points in latitude and longitude taking
     * into account height difference. If you are not interested in height
     * difference pass 0.0. Uses Haversine method as its base.
     * 
     * lat1, lon1 Start point lat2, lon2 End point el1 Start altitude in meters
     * el2 End altitude in meters
     * 
     * @returns Distance in Meters
     */
    public static Double CalculateDistance(Double lat1, Double lat2, Double lon1,
            Double lon2, Double el1, Double el2) {

        final Integer R = 6371; // Radius of the earth

        Double latDistance = Math.toRadians(lat2 - lat1);
        Double lonDistance = Math.toRadians(lon2 - lon1);
        Double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                        * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        Double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        Double distance = R * c * 1000; // convert to meters

        Double height = el1 - el2;

        distance = Math.pow(distance, 2) + Math.pow(height, 2);

        return Math.sqrt(distance);
    }

    @Override
    public VehicleState getById(String id) {
        return evamVehicleStateRepository.findById(id).orElseThrow(
                () -> new RuntimeException("No VehicleState found for id: %s".formatted(id)));
    }

}

