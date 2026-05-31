package com.example.applicationservice.repository;

import com.example.applicationservice.domain.ApplicationWorkFlow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ApplicationWorkFlowRepository extends JpaRepository<ApplicationWorkFlow, Integer> {
    // Employee: get all applications by employeeId
    List<ApplicationWorkFlow> findByEmployeeId(Long employeeId);

    // Employee: get all applications by status
    Optional<ApplicationWorkFlow> findByEmployeeIdAndStatus(Long employeeId, String status);

    // check if there is an ongoing application
    Optional<ApplicationWorkFlow> findByEmployeeIdAndStatusNot(Long employeeId, String status);

    // Employee + HR: get application by employeeId and applicationType
    Optional<ApplicationWorkFlow> findByEmployeeIdAndApplicationType(Long employeeId, String applicationType);

    // HR: get application workflows by employeeId and applicationType
    List<ApplicationWorkFlow> findAllByEmployeeIdAndApplicationType(Long employeeId, String applicationType);

    // HR: get all applications by status
    List<ApplicationWorkFlow> findByStatus(String status);

    // HR: get all applications by applicationType
    List<ApplicationWorkFlow> findByApplicationType(String applicationType);

    // HR: get all applications by applicationType and status
    List<ApplicationWorkFlow> findByApplicationTypeAndStatus(String applicationType, String status);
}
