package com.example.applicationservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class AdvanceWorkflowRequest {
    @NotBlank(message = "Current step is required")
    @Pattern(
            regexp = "ONBOARDING_SUBMITTED|I983_DOWNLOADED|I983_UPLOADED|I20_UPLOADED|OPT_RECEIPT_UPLOADED|OPT_EAD_UPLOADED|HR_REVIEW",
            message = "Current step is not supported"
    )
    private String currentStep;

    @Size(max = 1000, message = "Comment cannot exceed 1000 characters")
    private String comment;
}
