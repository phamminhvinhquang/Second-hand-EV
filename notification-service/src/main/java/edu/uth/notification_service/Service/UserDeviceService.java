
package edu.uth.notification_service.Service;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.uth.notification_service.DTO.RegisterDeviceDTO;
import edu.uth.notification_service.Model.UserDeviceToken;
import edu.uth.notification_service.Repository.UserDeviceTokenRepository;

@Service
public class UserDeviceService {

    @Autowired
    private UserDeviceTokenRepository userDeviceTokenRepository;

    /**
     *  NÂNG CẤP: Logic đăng ký (chạy khi user đăng nhập)
     */
    @Transactional
    public void registerOrUpdateDevice(RegisterDeviceDTO dto) {
        if (dto.getUserId() == null || dto.getToken() == null || dto.getToken().isEmpty()) {
            throw new IllegalArgumentException("UserId và Token là bắt buộc.");
        }

        // 1. Tìm xem cặp (user, token) này đã tồn tại chưa
        Optional<UserDeviceToken> existingPair = userDeviceTokenRepository
                .findByUserIdAndDeviceToken(dto.getUserId(), dto.getToken());

        if (existingPair.isPresent()) {
            // 2. Nếu đã tồn tại -> Chỉ cập nhật thời gian
            UserDeviceToken token = existingPair.get();
            token.setUpdatedAt(new Date());
            userDeviceTokenRepository.save(token);
            System.out.println("Đã cập nhật thời gian cho token của User ID: " + dto.getUserId());
        } else {
            // 3. Nếu chưa tồn tại -> Tạo cặp mới
            UserDeviceToken newToken = new UserDeviceToken(dto.getUserId(), dto.getToken());
            userDeviceTokenRepository.save(newToken);
            System.out.println("Đã thêm mới Device Token cho User ID: " + dto.getUserId());
        }
    }
    
    /**
     * Logic hủy đăng ký (chạy khi user đăng xuất)
     */
    @Transactional
    public void unregisterDevice(RegisterDeviceDTO dto) {
         if (dto.getUserId() == null || dto.getToken() == null || dto.getToken().isEmpty()) {
            System.err.println("Bỏ qua unregister: UserId hoặc Token rỗng.");
            return;
        }

        // 1. Tìm chính xác cặp (user, token)
        Optional<UserDeviceToken> existingPair = userDeviceTokenRepository
                .findByUserIdAndDeviceToken(dto.getUserId(), dto.getToken());

        // 2. Nếu tìm thấy -> Xóa nó
        if (existingPair.isPresent()) {
            userDeviceTokenRepository.delete(existingPair.get());
            System.out.println("Đã xóa Device Token của User ID: " + dto.getUserId());
        } else {
            System.out.println("Không tìm thấy token để xóa cho User ID: " + dto.getUserId());
        }
    }

    /**
     * (Giữ nguyên) Dùng để gửi thông báo
     */
    public List<String> getTokensByUserId(Long userId) {
        List<UserDeviceToken> tokens = userDeviceTokenRepository.findByUserId(userId);
        return tokens.stream()
                .map(UserDeviceToken::getDeviceToken)
                .collect(Collectors.toList());
    }
}