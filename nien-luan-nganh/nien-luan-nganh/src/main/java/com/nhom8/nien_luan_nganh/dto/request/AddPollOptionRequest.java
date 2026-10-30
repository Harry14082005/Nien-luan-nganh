package com.nhom8.nien_luan_nganh.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AddPollOptionRequest {
    @NotBlank
    private String optionText;
}
