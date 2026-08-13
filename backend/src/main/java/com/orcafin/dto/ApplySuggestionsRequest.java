package com.orcafin.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class ApplySuggestionsRequest {
    @NotNull
    private List<UUID> approvedSuggestionIds;
}
