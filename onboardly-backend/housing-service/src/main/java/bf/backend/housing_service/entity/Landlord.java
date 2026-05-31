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
@Table(name = "landlord")
public class Landlord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String cellPhone;

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
