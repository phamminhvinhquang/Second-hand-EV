package edu.uth.listingservice.Exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception này khi được "ném" (throw) trong Controller hoặc Service,
 * Spring Boot sẽ tự động chặn nó lại và trả về một response HTTP 404 NOT_FOUND.
 */
@ResponseStatus(value = HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}