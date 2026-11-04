package com.nhom8.nien_luan_nganh.dto.response;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileUploadResponse {
    private String fileUrl;
    private String originName;
    private String contentType;
    private long size;
}
