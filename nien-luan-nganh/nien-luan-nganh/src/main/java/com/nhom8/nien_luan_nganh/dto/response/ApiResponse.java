package com.nhom8.nien_luan_nganh.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    @Builder.Default
    private int status = 200;

    private String message;

    private T data;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    // Xu ly thanh cong va gui du lieu cho client
    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
                .status(200)
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    // Thanh cong nhung khong tra ve du lieu
    public static <T> ApiResponse<T> success(T data) {
        return success(data, "Thao tác thành công");
    }

    // Tra ve thong bao cho Delete va update
    public static <T> ApiResponse<T> message(String message) {
        return ApiResponse.<T>builder()
                .status(200)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }

    // Dung de dong goi loi truoc khi gui ve client
    public static <T> ApiResponse<T> error(int status, String message) {
        return ApiResponse.<T>builder()
                .status(status)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }

    // Lỗi mặc định 400 Bad Request (rất hay dùng trong GlobalExceptionHandler)
    public static <T> ApiResponse<T> error(String message) {
        return error(400, message);
    }
}