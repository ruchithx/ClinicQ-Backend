package com.dispensary.dms.patient.dto;

import com.dispensary.dms.common.web.PageResponse;
import com.dispensary.dms.consultation.dto.ConsultationResponse;

import java.util.List;

public record MedicalHistoryResponse(
        PatientResponse patient,
        List<ConditionSummary> conditions,
        PageResponse<ConsultationResponse> consultations) {
}
