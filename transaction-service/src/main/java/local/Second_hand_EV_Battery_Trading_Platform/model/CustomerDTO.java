package local.Second_hand_EV_Battery_Trading_Platform.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerDTO {
    private String fullName;
    private String phone;
    private String email;
    private String address;
}
