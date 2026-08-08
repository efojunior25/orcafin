package com.orcafin.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WhatsappMessageRequest {
    @NotBlank
    private String phone;

    /** "TEXT" ou "AUDIO" */
    @NotBlank
    private String type;

    private String text;

    private String audioBase64;
}
