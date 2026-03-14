package com.dedalus.amphi_integration.service.impl;

import com.dedalus.amphi_integration.repository.AmphiAssignmentHistoryRepository;
import com.dedalus.amphi_integration.repository.AmphiAssignmentRepository;
import com.dedalus.amphi_integration.repository.AmphiDestinationRepository;
import com.dedalus.amphi_integration.repository.AmphiStateEntryRepository;
import com.dedalus.amphi_integration.repository.AmphiSymbolRepository;
import com.dedalus.amphi_integration.repository.EvamMethaneReportRepository;
import com.dedalus.amphi_integration.repository.EvamOperationListRepository;
import com.dedalus.amphi_integration.repository.EvamOperationRepository;
import com.dedalus.amphi_integration.repository.EvamRakelStateRepository;
import com.dedalus.amphi_integration.repository.EvamTripLocationHistoryRepository;
import com.dedalus.amphi_integration.repository.EvamVehicleStateRepository;
import com.dedalus.amphi_integration.repository.EvamVehicleStatusRepository;
import com.dedalus.amphi_integration.repository.OperationDistanceRepository;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class RepositoryDataLoadService {

    private final AmphiAssignmentHistoryRepository amphiAssignmentHistoryRepository;
    private final AmphiAssignmentRepository amphiAssignmentRepository;
    private final AmphiDestinationRepository amphiDestinationRepository;
    private final AmphiStateEntryRepository amphiStateEntryRepository;
    private final AmphiSymbolRepository amphiSymbolRepository;
    private final EvamMethaneReportRepository evamMethaneReportRepository;
    private final EvamOperationListRepository evamOperationListRepository;
    private final EvamOperationRepository evamOperationRepository;
    private final EvamRakelStateRepository evamRakelStateRepository;
    private final EvamTripLocationHistoryRepository evamTripLocationHistoryRepository;
    private final EvamVehicleStateRepository evamVehicleStateRepository;
    private final EvamVehicleStatusRepository evamVehicleStatusRepository;
    private final OperationDistanceRepository operationDistanceRepository;

    public RepositoryDataLoadService(
            AmphiAssignmentHistoryRepository amphiAssignmentHistoryRepository,
            AmphiAssignmentRepository amphiAssignmentRepository,
            AmphiDestinationRepository amphiDestinationRepository,
            AmphiStateEntryRepository amphiStateEntryRepository,
            AmphiSymbolRepository amphiSymbolRepository,
            EvamMethaneReportRepository evamMethaneReportRepository,
            EvamOperationListRepository evamOperationListRepository,
            EvamOperationRepository evamOperationRepository,
            EvamRakelStateRepository evamRakelStateRepository,
            EvamTripLocationHistoryRepository evamTripLocationHistoryRepository,
            EvamVehicleStateRepository evamVehicleStateRepository,
            EvamVehicleStatusRepository evamVehicleStatusRepository,
            OperationDistanceRepository operationDistanceRepository) {
        this.amphiAssignmentHistoryRepository = amphiAssignmentHistoryRepository;
        this.amphiAssignmentRepository = amphiAssignmentRepository;
        this.amphiDestinationRepository = amphiDestinationRepository;
        this.amphiStateEntryRepository = amphiStateEntryRepository;
        this.amphiSymbolRepository = amphiSymbolRepository;
        this.evamMethaneReportRepository = evamMethaneReportRepository;
        this.evamOperationListRepository = evamOperationListRepository;
        this.evamOperationRepository = evamOperationRepository;
        this.evamRakelStateRepository = evamRakelStateRepository;
        this.evamTripLocationHistoryRepository = evamTripLocationHistoryRepository;
        this.evamVehicleStateRepository = evamVehicleStateRepository;
        this.evamVehicleStatusRepository = evamVehicleStatusRepository;
        this.operationDistanceRepository = operationDistanceRepository;
    }

    public Map<String, Integer> loadAllFromDisk() {
        Map<String, Integer> loadedEntityCounts = new LinkedHashMap<>();
        loadedEntityCounts.put("AssignmentHistory", amphiAssignmentHistoryRepository.reloadFromDisk());
        loadedEntityCounts.put("Assignment", amphiAssignmentRepository.reloadFromDisk());
        loadedEntityCounts.put("Destination", amphiDestinationRepository.reloadFromDisk());
        loadedEntityCounts.put("StateEntry", amphiStateEntryRepository.reloadFromDisk());
        loadedEntityCounts.put("Symbol", amphiSymbolRepository.reloadFromDisk());
        loadedEntityCounts.put("MethaneReport", evamMethaneReportRepository.reloadFromDisk());
        loadedEntityCounts.put("OperationList", evamOperationListRepository.reloadFromDisk());
        loadedEntityCounts.put("Operation", evamOperationRepository.reloadFromDisk());
        loadedEntityCounts.put("RakelState", evamRakelStateRepository.reloadFromDisk());
        loadedEntityCounts.put("TripLocationHistory", evamTripLocationHistoryRepository.reloadFromDisk());
        loadedEntityCounts.put("VehicleState", evamVehicleStateRepository.reloadFromDisk());
        loadedEntityCounts.put("VehicleStatus", evamVehicleStatusRepository.reloadFromDisk());
        loadedEntityCounts.put("OperationDistance", operationDistanceRepository.reloadFromDisk());
        return loadedEntityCounts;
    }
}