package bf.backend.housing_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "facility_report")
public class FacilityReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "facility_id", nullable = false)
    private Facility facility;

    @Column(nullable = false)
    private Long employeeId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportStatus status;

    private LocalDateTime createDate;

    private LocalDateTime lastModificationDate;

    @PrePersist
    public void prePersist() {
        this.createDate = LocalDateTime.now();
        this.lastModificationDate = LocalDateTime.now();

        if (this.status == null) {
            this.status = ReportStatus.OPEN;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.lastModificationDate = LocalDateTime.now();
    }
}
