package com.example.applicationservice.repository;

import com.example.applicationservice.domain.DigitalDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DigitalDocumentRepository extends JpaRepository<DigitalDocument, Integer> {

    List<DigitalDocument> findByApplicationWorkFlowId(Integer applicationId);

    // Employee + HR: get documents by applicationId and type
    List<DigitalDocument> findByApplicationWorkFlowIdAndType(Integer applicationId, String type);

    Optional<DigitalDocument> findFirstByApplicationWorkFlowIdAndType(Integer applicationId, String type);

    List<DigitalDocument> findByApplicationWorkFlowEmployeeIdAndApplicationWorkFlowApplicationType(
            Long employeeId, String applicationType);

    List<DigitalDocument> findByApplicationWorkFlowIdAndIsRequired(Integer applicationId, Integer isRequired);

    List<DigitalDocument> findByType(String type);
}