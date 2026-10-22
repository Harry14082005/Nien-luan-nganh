package com.nhom8.nien_luan_nganh.dto.response;

import lombok.*;
import org.springframework.data.domain.Page;

import java.util.Collections;
import java.util.List;

/**
 * Generic DTO đóng gói dữ liệu phân trang (Pagination) chuẩn cho toàn bộ hệ thống.
 * Giúp client (Vue.js / Mobile) dễ dàng hiển thị danh sách và thanh chuyển trang.
 *
 * @param <T> Kiểu dữ liệu của phần tử trong trang (ví dụ UserResponse, RoomResponse,...)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PageResponse<T> {

    private List<T> content;
    private int pageNo;
    private int pageSize;
    private long totalElements;
    private int totalPages;
    private boolean last;

    /**
     * Chuyển đổi từ Spring Data Page sang PageResponse.
     */
    public static <T> PageResponse<T> from(Page<T> page) {
        return PageResponse.<T>builder()
                .content(page.getContent())
                .pageNo(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }

    /**
     * Tạo PageResponse rỗng khi không có dữ liệu hoặc từ khóa trống.
     */
    public static <T> PageResponse<T> empty(int pageNo, int pageSize) {
        return PageResponse.<T>builder()
                .content(Collections.emptyList())
                .pageNo(pageNo)
                .pageSize(pageSize)
                .totalElements(0)
                .totalPages(0)
                .last(true)
                .build();
    }
}
