package bf.backend.employee_service.dto.response;

import bf.backend.employee_service.entity.StemOptStep;

import java.util.List;

public record StemOptProgressResponse(
        StemOptStep currentStep,
        StemOptStep nextStep,
        List<DocumentResponse> uploadedDocuments,
        boolean canDownloadI983
) {}
