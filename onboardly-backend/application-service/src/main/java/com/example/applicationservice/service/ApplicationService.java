package com.example.applicationservice.service;

import com.example.applicationservice.dto.request.ApplicationRequest;
import com.example.applicationservice.dto.request.ApplicationReviewRequest;
import com.example.applicationservice.dto.request.AdvanceWorkflowRequest;
import com.example.applicationservice.dto.response.ApplicationResponse;

import java.util.List;

public interface ApplicationService {
    // Employee: submit a new onboarding or OPT_STEM application
    ApplicationResponse createApplication(ApplicationRequest request);

    // Employee + HR: get application details by application id
    ApplicationResponse getApplicationById(Integer id);

    // Employee: get all applications submitted by a specific employee
    List<ApplicationResponse> getApplicationsByEmployeeId(Long employeeId);

    // HR: get applications by Auth userId and application type
    List<ApplicationResponse> getApplicationsByEmployeeIdAndType(Long employeeId, String applicationType);

    // HR: get all applications in the system
    List<ApplicationResponse> getAllApplications();

    // HR: get all applications filtered by status (e.g. Open, Rejected, Completed)
    List<ApplicationResponse> getApplicationsByStatus(String status);

    // HR: get all applications filtered by type (e.g. ONBOARDING, OPT_STEM)
    List<ApplicationResponse> getApplicationsByType(String applicationType);

    // HR: approve or reject an application with optional comment
    ApplicationResponse reviewApplication(Integer id, ApplicationReviewRequest request);

    // EmployeeService: advance workflow step when onboarding or OPT/STEM progresses
    ApplicationResponse advanceWorkflow(Integer id, AdvanceWorkflowRequest request);

    // Kafka integration: update the open OPT_STEM workflow by Auth userId
    ApplicationResponse advanceOptStemWorkflowByUserId(Long userId, AdvanceWorkflowRequest request);

    // Kafka integration: complete or reject the open OPT_STEM workflow by Auth userId
    ApplicationResponse reviewOptStemWorkflowByUserId(Long userId, String status, String comment);
}
