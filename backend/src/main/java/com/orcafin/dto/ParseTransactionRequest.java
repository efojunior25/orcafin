package com.orcafin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ParseTransactionRequest {

    @NotBlank
    @Size(max = 500, message = "Texto muito longo (máximo 500 caracteres).")
    private String text;
}
