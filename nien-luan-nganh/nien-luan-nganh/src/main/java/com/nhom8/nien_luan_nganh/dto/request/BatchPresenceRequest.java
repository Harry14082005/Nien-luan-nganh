package com.nhom8.nien_luan_nganh.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.Set;
import java.util.UUID;

/**
 * DTO yêu cầu tra cứu trạng thái Đang online / Vừa mới truy cập hàng loạt (Batch Presence).
 * - Sử dụng Set<UUID> để tự động khử trùng lặp (Deduplication) ngay khi Jackson deserialize mảng JSON.
 * - Giới hạn kích thước tối đa 100 phần tử để ngăn ngừa tấn công từ chối dịch vụ (DDoS) và tràn bộ nhớ (OOM).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BatchPresenceRequest {

    @NotEmpty(message = "Danh sách userId không được để trống")
    @Size(max = 100, message = "Mỗi lượt kiểm tra trạng thái chỉ được tối đa 100 người dùng")
    private Set<UUID> userIds;
}

