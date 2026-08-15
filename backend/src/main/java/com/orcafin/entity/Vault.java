package com.orcafin.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "vaults")
@Getter
@Setter
@NoArgsConstructor
public class Vault {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private BigDecimal balance = BigDecimal.ZERO;

    /** Taxa de rendimento em % ao ano (ex: 12.5 = 12,5% a.a.), informada manualmente pelo usuário. */
    @Column(nullable = false)
    private BigDecimal annualRate = BigDecimal.ZERO;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();
}
