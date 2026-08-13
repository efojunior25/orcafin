package com.orcafin.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class ImportStatementResponse {
    private UUID sessionId;
    private List<Suggestion> suggestions;
}
