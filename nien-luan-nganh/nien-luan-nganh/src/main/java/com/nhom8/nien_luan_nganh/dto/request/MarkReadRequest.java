package com.nhom8.nien_luan_nganh.dto.request;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MarkReadRequest {
    private UUID messageId;
}
