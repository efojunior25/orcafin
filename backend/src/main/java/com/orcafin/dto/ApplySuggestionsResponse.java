package com.orcafin.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ApplySuggestionsResponse {
    private int applied;
    private String message;
}
