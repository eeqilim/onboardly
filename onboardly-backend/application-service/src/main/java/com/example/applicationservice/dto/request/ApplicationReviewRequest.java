package com.example.applicationservice.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class ApplicationReviewRequest {
    @NotBlank(message = "Status is required")
    private String status;

    @Size(max = 1000, message = "Comment cannot exceed 1000 characters")
    private String comment;

    @Email(message = "Employee email must be valid")
    private String employeeEmail;
}
