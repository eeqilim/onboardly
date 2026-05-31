package com.example.applicationservice.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DocumentTemplateResponse {
    private String applicationType;
    private String type;
    private Integer isRequired;
    private String path;
    private String description;
    private String title;
}
