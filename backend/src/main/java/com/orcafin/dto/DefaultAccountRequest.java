package com.orcafin.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class DefaultAccountRequest {
    @NotNull
    private UUID accountId;
}
