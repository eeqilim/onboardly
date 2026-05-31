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
@Table(name = "facility_report_detail")
public class FacilityReportDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "facility_report_id", nullable = false)
    private FacilityReport facilityReport;

    @Column(nullable = false)
    private Long employeeId;

    @Column(nullable = false, length = 2000)
    private String comment;

    private LocalDateTime createDate;

    private LocalDateTime lastModificationDate;

    @PrePersist
    public void prePersist() {
        this.createDate = LocalDateTime.now();
        this.lastModificationDate = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.lastModificationDate = LocalDateTime.now();
    }
}
