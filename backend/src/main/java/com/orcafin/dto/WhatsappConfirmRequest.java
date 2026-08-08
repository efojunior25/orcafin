package com.orcafin.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WhatsappConfirmRequest {
    @NotBlank
    private String phone;

    private boolean confirm;
}
