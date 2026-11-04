package com.nhom8.nien_luan_nganh.service;

import com.nhom8.nien_luan_nganh.config.FileStorageConfig;
import com.nhom8.nien_luan_nganh.exception.AppException;
import com.nhom8.nien_luan_nganh.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FileValidationService {
    private final FileStorageConfig config;
    public void validateImage(MultipartFile file){
        validateNotEmpty(file);
        validateSize(file, config.getMaxFileSize());
        validateContentType(file, config.getAllowedImageTypes());
    }

    public void validateAttachment(MultipartFile file){
        validateNotEmpty(file);
        validateSize(file, config.getMaxFileSize());
        validateContentType(file, config.getAllowedFileTypes());
    }

    private void validateNotEmpty(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_INPUT);
        }
    }

    private void validateSize(MultipartFile file, long maxSize) {
        if (file.getSize() > maxSize) {
            throw new AppException(ErrorCode.INVALID_INPUT);
        }
    }

    private void validateContentType(MultipartFile file, List<String> allowed) {
        String contentType = file.getContentType();
        if (contentType == null || !allowed.contains(contentType)) {
            throw new AppException(ErrorCode.INVALID_INPUT);
        }
    }
}
