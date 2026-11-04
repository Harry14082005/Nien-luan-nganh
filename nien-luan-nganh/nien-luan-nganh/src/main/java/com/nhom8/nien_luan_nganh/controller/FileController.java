package com.nhom8.nien_luan_nganh.controller;

import com.nhom8.nien_luan_nganh.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Files;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/files")
public class FileController {
    private final FileStorageService fileStorageService;

    @GetMapping
    public ResponseEntity<Resource> loadFile(@RequestParam String storageKey){
        Resource resource = fileStorageService.loadFile(storageKey);

        MediaType contentType = MediaType.APPLICATION_OCTET_STREAM; //dữ liệu nhị phân chung chung
        // -> cần phân loại ra

        try{
            String detectedType = Files.probeContentType(resource.getFile().toPath());
            // -> nhận diện file dựa trên đường dẫn
            // detectedType có thể là image/jpeg, application/pdf ,...
            if(detectedType != null){
                contentType = MediaType.parseMediaType(detectedType);
            }
        } catch (Exception ignored){

        }

        return ResponseEntity.ok()
                .contentType(contentType) //cho trình duyệt biết dữ liệu trả về là gì
                .header(
                    HttpHeaders.CONTENT_DISPOSITION,
                "inline; filename=\"" + resource.getFilename() + "\""
        ).body(resource);
    }
}
