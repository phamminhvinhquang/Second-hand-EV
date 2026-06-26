package edu.uth.listingservice.Service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {

    private final Path rootLocation;

    // Inject giá trị từ application.properties hoặc biến môi trường
    // Mặc định là "uploads" (cho local), Docker sẽ ghi đè thành "/app/uploads"
    public FileStorageService(@Value("${app.upload.dir:uploads}") String uploadDir) {
        this.rootLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.rootLocation);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize storage location", e);
        }
    }

    public String store(MultipartFile file) {
        if (file.isEmpty()) {
            throw new RuntimeException("Failed to store empty file.");
        }

        String extension = getFileExtension(file.getOriginalFilename());
        String newFileName = UUID.randomUUID().toString() + "." + extension;

        try {
            // Resolve đường dẫn file đích
            Path destinationFile = this.rootLocation.resolve(Paths.get(newFileName))
                    .normalize().toAbsolutePath();

            // Bảo mật: Kiểm tra file có nằm ngoài thư mục cho phép không
            if (!destinationFile.getParent().equals(this.rootLocation)) {
                throw new RuntimeException("Cannot store file outside current directory.");
            }

            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
            }

            // Trả về đường dẫn URL công khai
            return "/uploads/" + newFileName;

        } catch (IOException e) {
            throw new RuntimeException("Failed to store file.", e);
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf(".") == -1) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1);
    }

    public void delete(String filePath) {
        if (filePath == null || filePath.isBlank()) {
            return;
        }
        try {
            String filename = filePath.substring(filePath.lastIndexOf("/") + 1);
            Path file = rootLocation.resolve(filename);
            Files.deleteIfExists(file);
        } catch (IOException e) {
            System.err.println("Could not delete file: " + filePath + ". Error: " + e.getMessage());
        }
    }
}