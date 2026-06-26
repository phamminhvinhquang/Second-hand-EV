package local.wallet_service.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import local.wallet_service.model.StaffSalary;

public interface StaffSalaryRepository extends JpaRepository<StaffSalary, Long> {
    Optional<StaffSalary> findByUserId(Long userId);
    List<StaffSalary> findByStatus(String status);
}
