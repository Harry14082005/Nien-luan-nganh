package com.nhom8.nien_luan_nganh.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "app.storage")
@Getter
@Setter
public class FileStorageConfig {
    private String location; //uploads
    private long maxImageSize;
    private long maxFileSize;

    private List<String> allowedImageTypes;
    private List<String> allowedFileTypes;
}
