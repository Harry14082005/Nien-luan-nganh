package com.nhom8.nien_luan_nganh.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * DTO phản hồi kết quả tìm kiếm tin nhắn dạng Lightweight Metadata.
 * Chỉ trả về tổng số kết quả (totalMatches) và danh sách UUID (matchIds)
 * giúp giảm thiểu băng thông mạng và tối ưu bộ nhớ phía Client.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageSearchResponse {
    private int totalMatches;
    private List<UUID> matchIds;
}

