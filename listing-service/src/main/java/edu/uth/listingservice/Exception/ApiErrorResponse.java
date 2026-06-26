// Đặt trong package: edu.uth.listingservice.Exception
package edu.uth.listingservice.Exception;

import java.time.LocalDateTime;

// Đây là DTO định nghĩa cấu trúc JSON Lỗi
public class ApiErrorResponse {

    private int status;
    private String message;
    private String path;
    private LocalDateTime timestamp;
    private String errorCode;

    // Constructor
    public ApiErrorResponse(int status, String message, String path, String errorCode) {
        this.status = status;
        this.message = message;
        this.path = path;
        this.errorCode = errorCode;
        this.timestamp = LocalDateTime.now();
    }

    // Getters (BẮT BUỘC có để Jackson hoạt động)
    public int getStatus() { return status; }
    public String getMessage() { return message; }
    public String getPath() { return path; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getErrorCode() { return errorCode; }
}