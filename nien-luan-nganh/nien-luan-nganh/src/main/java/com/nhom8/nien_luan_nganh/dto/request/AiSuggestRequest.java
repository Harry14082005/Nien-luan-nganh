package com.nhom8.nien_luan_nganh.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class AiSuggestRequest {

    public enum SuggestionType {
        POLL,
        REMINDER
    }

    @NotNull
    private SuggestionType type;

    @NotNull
    private UUID anchorMessageId;
}

