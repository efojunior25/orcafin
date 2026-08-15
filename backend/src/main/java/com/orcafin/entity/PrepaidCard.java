package com.orcafin.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "prepaid_cards")
@Getter
@Setter
@NoArgsConstructor
public class PrepaidCard {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PrepaidCardType type;

    @Enumerated(EnumType.STRING)
    private TransitSubtype subtype;

    @Column(nullable = false)
    private BigDecimal balance = BigDecimal.ZERO;

    /** Dia do mês em que o benefício é recarregado automaticamente pela empresa (null = sem recarga automática). */
    private Integer rechargeDay;

    /** Valor recarregado no dia acima. Editável a qualquer momento para ajustar o próximo ciclo. */
    private BigDecimal rechargeAmount;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();
}
