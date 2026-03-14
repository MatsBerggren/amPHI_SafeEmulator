package com.dedalus.amphi_integration.model.evam;

import java.time.LocalDateTime;
import java.util.Optional;

import com.dedalus.amphi_integration.util.FlexibleIntegerDeserializer;
import com.dedalus.amphi_integration.util.FlexibleStringDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.gson.annotations.JsonAdapter;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Operation {
    private String id;
    private String amPHIUniqueId;
    private String operationID;
    private String name;
    private LocalDateTime sendTime;
    private LocalDateTime createdTime;
    private LocalDateTime endTime;
    private LocalDateTime acceptedTime;
    private String callCenterId;
    private String caseFolderId;
    private String transmitterCode;
    private String alarmCategory;
    private String alarmEventCode;
    private String medicalCommander;
    private String medicalIncidentOfficer;
    private String attachedCustomerObject;
    private String alarmEventText;
    private String additionalInfo;
    private String keyNumber;
    private String electronicKey;
    private String radioGroupMain;
    private String radioGroupSecondary;
    private String additionalCoordinationInformation;
    private OperationPriority[] availablePriorities;
    private String patientName;
    private String patientUID;
    private VehicleStatus vehicleStatus;
    private DestinationSiteLocation destinationSiteLocation;
    private DestinationControlPointLocation breakpointLocation;
    private HospitalLocation[] availableHospitalLocations;
    private String header1;
    private String header2;
    private String eventInfo;
    private String caseInfo;
    @JsonAdapter(FlexibleStringDeserializer.class)
    @JsonDeserialize(using = FlexibleStringDeserializer.class)
    private String selectedHospital;
    @JsonAdapter(FlexibleIntegerDeserializer.class)
    @JsonDeserialize(using = FlexibleIntegerDeserializer.class)
    private Integer selectedPriority;
    private OperationState operationState;
    private LeavePatientLocation leavePatientLocation;
    private String assignedResourceMissionNo;
    private OperationUnit[] operationUnits;

    public String getFullId() {
        String missionNo = "0";
        if (this.assignedResourceMissionNo != null) {
            String[] textSplit = this.assignedResourceMissionNo.split("\u0016");
            if (textSplit.length > 1) {
                missionNo = textSplit[1];
            }
        }
        return this.callCenterId + ":" + this.caseFolderId + ":" + this.operationID + ":" + missionNo;
    }

    public void updateFrom(Operation other) {
        Optional.ofNullable(other.getAmPHIUniqueId()).ifPresent(this::setAmPHIUniqueId);
        Optional.ofNullable(other.getOperationID()).ifPresent(this::setOperationID);
        Optional.ofNullable(other.getName()).ifPresent(this::setName);
        Optional.ofNullable(other.getSendTime()).ifPresent(this::setSendTime);
        Optional.ofNullable(other.getCreatedTime()).ifPresent(this::setCreatedTime);
        Optional.ofNullable(other.getEndTime()).ifPresent(this::setEndTime);
        Optional.ofNullable(other.getAcceptedTime()).ifPresent(this::setAcceptedTime);
        Optional.ofNullable(other.getCallCenterId()).ifPresent(this::setCallCenterId);
        Optional.ofNullable(other.getCaseFolderId()).ifPresent(this::setCaseFolderId);
        Optional.ofNullable(other.getTransmitterCode()).ifPresent(this::setTransmitterCode);
        Optional.ofNullable(other.getAlarmCategory()).ifPresent(this::setAlarmCategory);
        Optional.ofNullable(other.getAlarmEventCode()).ifPresent(this::setAlarmEventCode);
        Optional.ofNullable(other.getMedicalCommander()).ifPresent(this::setMedicalCommander);
        Optional.ofNullable(other.getMedicalIncidentOfficer()).ifPresent(this::setMedicalIncidentOfficer);
        Optional.ofNullable(other.getAttachedCustomerObject()).ifPresent(this::setAttachedCustomerObject);
        Optional.ofNullable(other.getAlarmEventText()).ifPresent(this::setAlarmEventText);
        Optional.ofNullable(other.getAdditionalInfo()).ifPresent(this::setAdditionalInfo);
        Optional.ofNullable(other.getKeyNumber()).ifPresent(this::setKeyNumber);
        Optional.ofNullable(other.getElectronicKey()).ifPresent(this::setElectronicKey);
        Optional.ofNullable(other.getRadioGroupMain()).ifPresent(this::setRadioGroupMain);
        Optional.ofNullable(other.getRadioGroupSecondary()).ifPresent(this::setRadioGroupSecondary);
        Optional.ofNullable(other.getAdditionalCoordinationInformation()).ifPresent(this::setAdditionalCoordinationInformation);
        Optional.ofNullable(other.getAvailablePriorities()).ifPresent(this::setAvailablePriorities);
        Optional.ofNullable(other.getPatientName()).ifPresent(this::setPatientName);
        Optional.ofNullable(other.getPatientUID()).ifPresent(this::setPatientUID);
        Optional.ofNullable(other.getVehicleStatus()).ifPresent(this::setVehicleStatus);
        Optional.ofNullable(other.getDestinationSiteLocation()).ifPresent(this::setDestinationSiteLocation);
        Optional.ofNullable(other.getBreakpointLocation()).ifPresent(this::setBreakpointLocation);
        Optional.ofNullable(other.getAvailableHospitalLocations()).ifPresent(this::setAvailableHospitalLocations);
        Optional.ofNullable(other.getHeader1()).ifPresent(this::setHeader1);
        Optional.ofNullable(other.getHeader2()).ifPresent(this::setHeader2);
        Optional.ofNullable(other.getEventInfo()).ifPresent(this::setEventInfo);
        Optional.ofNullable(other.getCaseInfo()).ifPresent(this::setCaseInfo);
        Optional.ofNullable(other.getSelectedHospital()).ifPresent(this::setSelectedHospital);
        Optional.ofNullable(other.getSelectedPriority()).ifPresent(this::setSelectedPriority);
        Optional.ofNullable(other.getOperationState()).ifPresent(this::setOperationState);
        Optional.ofNullable(other.getLeavePatientLocation()).ifPresent(this::setLeavePatientLocation);
        Optional.ofNullable(other.getAssignedResourceMissionNo()).ifPresent(this::setAssignedResourceMissionNo);
        Optional.ofNullable(other.getOperationUnits()).ifPresent(this::setOperationUnits);
    }
}
