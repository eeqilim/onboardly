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
@Table(name = "house")
public class House {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String address;

    @Column(nullable = false)
    private Integer maxOccupant;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "landlord_id", nullable = false)
    private Landlord landlord;

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
