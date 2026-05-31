package com.example.authserver.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "registration_tokens")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class RegistrationToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "token", nullable = false)
    private String token;

    @Column(name = "email")
    private String email;

    @Column(name = "expiration_date")
    private LocalDateTime expirationDate;

    @Column(name = "used_flag")
    private Integer usedFlag;

    @ManyToOne
    @JoinColumn(name = "create_by")
    private User createBy;

    @Column(name = "create_date", updatable = false)
    private LocalDateTime createDate;

    @Column(name = "last_modification_date")
    private LocalDateTime lastModificationDate;

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expirationDate);
    }
}
