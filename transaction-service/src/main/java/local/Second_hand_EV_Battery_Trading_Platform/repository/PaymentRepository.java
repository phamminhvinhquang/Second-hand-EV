package local.Second_hand_EV_Battery_Trading_Platform.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import local.Second_hand_EV_Battery_Trading_Platform.entity.Payment;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByTransactionId(String transactionId);
}
