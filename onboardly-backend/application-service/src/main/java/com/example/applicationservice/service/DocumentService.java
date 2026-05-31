package com.example.applicationservice.service;

import com.example.applicationservice.dto.request.DocumentUploadRequest;
import com.example.applicationservice.dto.response.DocumentResponse;
import com.example.applicationservice.dto.response.DocumentTemplateResponse;

import java.util.List;

public interface DocumentService {
    // Employee: upload document
    DocumentResponse createDocumentMetadata(DocumentUploadRequest request);

    // Employee + HR: get document by id
    DocumentResponse getDocumentById(Integer id);

    // Employee + HR: get all documents by applicationId
    List<DocumentResponse> getDocumentsByApplicationId(Integer applicationId);

    // Employee + HR: get documents by applicationId and type
    List<DocumentResponse> getDocumentsByApplicationIdAndType(Integer applicationId, String type);

    // Employee + HR: get documents by employee auth userId and application type
    List<DocumentResponse> getDocumentsByEmployeeIdAndApplicationType(Long employeeId, String applicationType);

    // Employee: get documents by current user and application type
    List<DocumentResponse> getDocumentsByCurrentUserAndApplicationType(String applicationType);

    // Employee: check all required documents before submission
    void checkRequiredDocuments(Integer applicationId);

    // Employee + HR: get backend-managed template documents for an application type
    List<DocumentTemplateResponse> getDocumentTemplates(String applicationType);
}
