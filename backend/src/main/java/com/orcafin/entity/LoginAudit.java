package com.orcafin.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "login_audit")
@Getter
@Setter
@NoArgsConstructor
public class LoginAudit {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String ip;

    @Column(nullable = false)
    private boolean success;

    private String userAgent;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();
}
