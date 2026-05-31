package com.example.applicationservice.dto.request;

import jakarta.validation.constraints.Pattern;
import lombok.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class ApplicationRequest {
    @NotNull(message = "Employee ID is required")
    private Long employeeId;

    @NotBlank(message = "Application type is required")
    @Pattern(regexp = "ONBOARDING|OPT_STEM", message = "Application type must be ONBOARDING or OPT_STEM")
    private String applicationType;
}
