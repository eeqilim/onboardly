package com.example.applicationservice.dto.response;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class DocumentResponse {
    private Integer id;
    private Integer applicationId;
    private String type;
    private Integer isRequired;
    private String path;
    private String sourceDocumentId;
    private String description;
    private String title;
}
