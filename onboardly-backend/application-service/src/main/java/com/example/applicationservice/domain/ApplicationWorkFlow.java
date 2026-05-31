package com.example.applicationservice.domain;


import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "application_workflow")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class ApplicationWorkFlow {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Column(name = "create_date", nullable = false, updatable = false)
    private LocalDateTime createDate;

    @Column(name = "last_modification_date", nullable = false)
    private LocalDateTime lastModificationDate;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "current_step")
    private String currentStep;

    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;

    @Column(name = "application_type", nullable = false)
    private String applicationType;

    @OneToMany(mappedBy = "applicationWorkFlow", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<DigitalDocument> documents;
}
