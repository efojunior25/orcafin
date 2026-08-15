package com.orcafin.dto;

import com.orcafin.entity.PrepaidCardType;
import com.orcafin.entity.TransitSubtype;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class PrepaidCardRequest {

    @NotBlank
    private String name;

    @NotNull
    private PrepaidCardType type;

    /** Obrigatório apenas quando type = VALE_TRANSPORTE. */
    private TransitSubtype subtype;

    /** Dia do mês da recarga automática (opcional). */
    @Min(1)
    @Max(28)
    private Integer rechargeDay;

    /** Valor da recarga automática (opcional; exigido junto com rechargeDay). */
    private BigDecimal rechargeAmount;
}
