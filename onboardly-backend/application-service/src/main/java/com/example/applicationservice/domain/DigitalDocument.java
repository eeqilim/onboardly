package com.example.applicationservice.domain;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "digital_document")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class DigitalDocument {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id")
    private ApplicationWorkFlow applicationWorkFlow;

    @Column(name = "type", nullable = false)
    private String type;

    // 1 = required, 0 = optional
    @Column(name = "is_required", nullable = false)
    private Integer isRequired;

    @Column(name = "path", nullable = false)
    private String path;

    @Column(name = "source_document_id")
    private String sourceDocumentId;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "title", nullable = false)
    private String title;
}
