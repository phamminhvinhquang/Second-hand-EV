package edu.uth.listingservice.Controller;

import edu.uth.listingservice.Service.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


@RestController
@RequestMapping("/api/files")

public class FileUploadController {

    @Autowired
    private FileStorageService fileStorageService;

    @PostMapping("/upload")
    public ResponseEntity<List<String>> uploadFiles(@RequestParam("files") MultipartFile[] files) {
        List<String> urls = new ArrayList<>();
        Arrays.stream(files).forEach(file -> {
            String url = fileStorageService.store(file);
            urls.add(url);
        });
        return ResponseEntity.ok(urls);
    }
}
