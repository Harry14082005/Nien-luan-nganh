package com.nhom8.nien_luan_nganh.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO trả về thông tin lịch sử chỉnh sửa của một tin nhắn.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageHistoryResponse {

    private UUID messageHistoryId;
    private UUID messageId;
    private String content;
    private LocalDateTime editedAt;
}
