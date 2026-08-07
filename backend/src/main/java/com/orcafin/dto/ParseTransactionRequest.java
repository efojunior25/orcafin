package com.orcafin.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ParseTransactionRequest {

    @NotBlank
    private String text;
}
