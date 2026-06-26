package local.contract.controller;

import java.nio.file.Path;

import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/contracts")
public class ContractFileController {

    @GetMapping("/{fileName}")
    public ResponseEntity<FileSystemResource> getPdf(@PathVariable String fileName) {

        Path filePath = Path.of("/app/contracts/" + fileName);
        FileSystemResource file = new FileSystemResource(filePath.toFile());

        if (!file.exists()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .body(file);
    }
}
