package com.orcafin.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PhoneCodeResponse {
    private String code;
    private int expiresInMinutes;
}
