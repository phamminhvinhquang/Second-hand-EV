
package edu.uth.notification_service.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.uth.notification_service.DTO.RegisterDeviceDTO;
import edu.uth.notification_service.Service.UserDeviceService;

@RestController
@RequestMapping("/api/devices") // (Endpoint ví dụ, thay bằng của bạn)
public class UserDeviceController {

    @Autowired
    private UserDeviceService userDeviceService;

    // (Endpoint bạn đã có)
    @PostMapping("/register")
    public ResponseEntity<Void> registerDevice(@RequestBody RegisterDeviceDTO dto) {
        userDeviceService.registerOrUpdateDevice(dto);
        return ResponseEntity.ok().build();
    }

    //  NÂNG CẤP: Thêm endpoint này
    @PostMapping("/unregister")
    public ResponseEntity<Void> unregisterDevice(@RequestBody RegisterDeviceDTO dto) {
        userDeviceService.unregisterDevice(dto);
        return ResponseEntity.ok().build();
    }
}