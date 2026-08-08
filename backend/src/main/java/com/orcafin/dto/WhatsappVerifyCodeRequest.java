package com.orcafin.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WhatsappVerifyCodeRequest {
    @NotBlank
    private String phone;

    @NotBlank
    private String code;
}
