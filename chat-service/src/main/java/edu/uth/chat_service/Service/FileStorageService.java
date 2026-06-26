// File: src/main/java/edu/uth/chat_service/Service/FileStorageService.java
package edu.uth.chat_service.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.stereotype.Service;

@Service
public class FileStorageService {

    private final Path rootLocation = Paths.get("uploads");

    public void deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) return;

        try {
            //  SỬA LỖI: Không hard-code "/uploads/".
           
            String fileName = fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
            
            Path file = rootLocation.resolve(fileName);
            
            if (Files.exists(file)) {
                Files.delete(file);
                System.out.println("✅ Đã xóa file vật lý: " + fileName);
            } else {
                System.out.println("⚠️ File không tồn tại: " + fileName);
            }
        } catch (IOException e) {
            System.err.println("❌ Lỗi khi xóa file: " + e.getMessage());
        }
    }
}