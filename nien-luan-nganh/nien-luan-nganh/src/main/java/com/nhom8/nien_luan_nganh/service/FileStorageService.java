package com.nhom8.nien_luan_nganh.service;

import com.nhom8.nien_luan_nganh.config.FileStorageConfig;
import com.nhom8.nien_luan_nganh.exception.AppException;
import com.nhom8.nien_luan_nganh.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileStorageService {
    private final FileStorageConfig config;
    //lưu avatar
    public String saveAvatar(MultipartFile file){
        String fileName = UUID.randomUUID() + getExtension(file);

        Path dir = getBaseDir().resolve("avatars");
        // xây dựng /project/uploads/avatars

        return save(file, dir, fileName, "avatars/" + fileName);
        // trả về "avatars/gì đó....jpg"
    }

    //lưu ảnh tin nhắn
    public String saveMessageImage(MultipartFile file, UUID roomId, UUID messageId){
        String fileName = UUID.randomUUID() + getExtension(file);
        Path dir = getBaseDir().resolve("messages")
                .resolve(roomId.toString())
                .resolve(messageId.toString());
        return save(file, dir, fileName, "messages/" + roomId + "/" + messageId + "/" + fileName);
    }

    //lưu ảnh tin nhắn
    public String saveMessageFile(MultipartFile file, UUID roomId, UUID messageId){
        String fileName = UUID.randomUUID() + getExtension(file);
        Path dir = getBaseDir().resolve("messages")
                .resolve(roomId.toString())
                .resolve(messageId.toString());
        return save(file, dir, fileName, "messages/" + roomId + "/" + messageId + "/" + fileName);
    }

    //đọc file trả về user
    public Resource loadFile(String storageKey){
        try{
            Path filePath = getBaseDir().resolve(storageKey).normalize();

            //không cho truy cập file ngoài thư mục upload
            if(!filePath.startsWith(getBaseDir())){
                throw  new AppException(ErrorCode.UNAUTHORIZED);
            }

            Resource resource = new UrlResource(filePath.toUri());
            if(!resource.exists() || !resource.isReadable()){
                throw new AppException(ErrorCode.INVALID_INPUT);
            }
            return resource;
        } catch (Exception e){
            throw new AppException(ErrorCode.INVALID_INPUT);
        }
    }

    //xoá file (dổi avatar với thu hồi tin nhắn
    public void deleteFile(String storageKey){
        if(storageKey == null || storageKey.isBlank()){
            return;
        }
        try{
            Path filePath = getBaseDir().resolve(storageKey).normalize();
            //không cho truy cập file ngoài thư mục upload
            if(!filePath.startsWith(getBaseDir())){
                throw  new AppException(ErrorCode.UNAUTHORIZED);
            }
            Files.deleteIfExists(filePath);
            log.info("Deleted file: {}", storageKey);
        }catch (Exception e){
            log.warn("Cannot delete file {}: {}", storageKey, e.getMessage());
        }
    }

    private String save(MultipartFile file, Path dir, String fileName, String storageKey){
        try{
            Files.createDirectories(dir);
            Files.copy(
                    file.getInputStream(),
                    dir.resolve(fileName), //ghép tên thư mục + tên file -> đường dẫn để lưu trên máy
                    StandardCopyOption.REPLACE_EXISTING); //xử lý ghi đè nếu trùng mà hiếm khi
            log.info("Saved: {}", storageKey);
            return storageKey;
        } catch (Exception e){
            log.error("Save fail: {}", e.getMessage());
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
        }
    }

    //lấy đường dẫn cơ sở /uploads
    private Path getBaseDir(){
        return Paths.get(config.getLocation()) // "uploads"
                .toAbsolutePath() // ../uploads
                .normalize(); // loại bỏ dấu . hoặc .. trong đường dẫn
    }

    //lấy phần mở rộng
    private String getExtension(MultipartFile file){
        String original = file.getOriginalFilename();
        if(original != null && original.contains(".")){
            return original.substring(original.lastIndexOf(".")).toLowerCase();
        }

        return ""; //không có phần mwor rộng
    }
}
