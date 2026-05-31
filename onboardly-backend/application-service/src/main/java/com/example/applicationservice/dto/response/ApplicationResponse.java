package com.example.applicationservice.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class ApplicationResponse {
    private Integer id;
    private Long employeeId;
    private LocalDateTime createDate;
    private LocalDateTime lastModificationDate;
    private String status;
    private String currentStep;
    private String comment;
    private String applicationType;
    private List<DocumentResponse> documents;
}
