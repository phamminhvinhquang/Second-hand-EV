
package edu.uth.chat_service.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import edu.uth.chat_service.Model.ChatUser;

@Repository
public interface ChatUserRepository extends JpaRepository<ChatUser, Long> {
    // Mặc định đã có save(), findById()... nên không cần viết thêm gì
}