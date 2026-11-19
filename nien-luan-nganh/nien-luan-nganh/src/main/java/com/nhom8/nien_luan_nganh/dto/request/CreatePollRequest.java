package com.nhom8.nien_luan_nganh.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class CreatePollRequest {
    @NotBlank
    private String question;

    @NotNull
    @Size(min = 2, max = 10)
    private List<@NotBlank String> options;

    // -- GỢI Ý AI CHO POLL -- //
    // Danh sách id tin nhắn nguồn (nếu poll được tạo từ gợi ý AI)
    private List<UUID> sourceMessageIds;
}
