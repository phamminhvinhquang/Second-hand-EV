// Đặt trong package: edu.uth.listingservice.Exception
package edu.uth.listingservice.Exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Bắt lỗi 404 (Không tìm thấy)
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleResourceNotFound(
            ResourceNotFoundException ex, 
            HttpServletRequest request) {
        
        ApiErrorResponse errorResponse = new ApiErrorResponse(
            HttpStatus.NOT_FOUND.value(),       // 404
            ex.getMessage(),                    // "Không tìm thấy tin đăng này..."
            request.getRequestURI(),            // "/api/product-details/149"
            "RESOURCE_NOT_FOUND"                // Mã lỗi tùy chỉnh
        );

        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }
    
    /**
     * Bắt tất cả các lỗi 500 (Lỗi máy chủ)
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGenericException(
            Exception ex, 
            HttpServletRequest request) {

        ApiErrorResponse errorResponse = new ApiErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR.value(), // 500
            "Đã có lỗi máy chủ xảy ra, vui lòng thử lại sau.", // Thông báo an toàn
            request.getRequestURI(),
            "INTERNAL_SERVER_ERROR"
        );

        // Log lỗi chi tiết cho bạn xem
        ex.printStackTrace(); 

        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}