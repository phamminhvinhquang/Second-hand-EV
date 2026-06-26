package local.Second_hand_EV_Battery_Trading_Platform.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object (DTO) đại diện cho giỏ hàng trả về từ cart-service.
 * Dùng để deserialize JSON từ API: /api/carts/{id}
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartDTO {

    private Long id;              // ID giỏ hàng
    private Long customerId;      // ID khách hàng (nếu cart-service có trường này)
    private String productName;   // Tên sản phẩm
    private Double price;         // Giá sản phẩm
    private Double totalAmount;   // Tổng tiền giỏ hàng (nếu có nhiều item)
    private Long sellerId;
}
