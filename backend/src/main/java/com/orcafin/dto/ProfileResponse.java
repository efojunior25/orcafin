package com.orcafin.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@Getter
@AllArgsConstructor
public class ProfileResponse {
    private boolean phoneLinked;
    private String phoneNumber;
    private UUID defaultAccountId;
}
