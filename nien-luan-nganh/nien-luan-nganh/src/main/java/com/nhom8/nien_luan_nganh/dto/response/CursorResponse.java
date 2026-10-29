package com.nhom8.nien_luan_nganh.dto.response;

import lombok.*;

import java.util.List;
import java.util.UUID;

/**
 * Generic DTO đóng gói dữ liệu phân trang theo con trỏ (Cursor-based Pagination).
 * Hỗ trợ tải tin nhắn theo cơ chế cuộn vô tận (Infinite Scroll) tránh trùng lặp.
 *
 * @param <T> Kiểu dữ liệu phần tử (ví dụ MessageResponse)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CursorResponse<T> {

    private List<T> data;
    private UUID nextCursor;
    private boolean hasMore;
}
