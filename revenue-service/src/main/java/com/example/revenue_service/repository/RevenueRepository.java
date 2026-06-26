package com.example.revenue_service.repository;

import com.example.revenue_service.model.Revenue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public interface RevenueRepository extends JpaRepository<Revenue, Long> {

    boolean existsByPurchaseId(Long purchaseId);
    
    void deleteByPurchaseId(Long purchaseId);

    // ✅ SỬA: GROUP BY r.revenue_date thay vì GROUP BY date (alias)
    @Query(value = "SELECT DATE_FORMAT(r.revenue_date, '%Y-%m-%d') as date, SUM(r.amount) as total " +
            "FROM revenues r GROUP BY r.revenue_date ORDER BY r.revenue_date DESC", nativeQuery = true)
    List<Map<String, Object>> findDailyRevenue();

    // ✅ SỬA: GROUP BY YEAR(r.revenue_date), MONTH(r.revenue_date) thay vì alias
    @Query(value = "SELECT YEAR(r.revenue_date) as year, MONTH(r.revenue_date) as month, SUM(r.amount) as total " +
            "FROM revenues r GROUP BY YEAR(r.revenue_date), MONTH(r.revenue_date) " +
            "ORDER BY year DESC, month DESC", nativeQuery = true)
    List<Map<String, Object>> findMonthlyRevenue();

    // ✅ SỬA: GROUP BY YEAR(r.revenue_date) thay vì alias
    @Query(value = "SELECT YEAR(r.revenue_date) as year, SUM(r.amount) as total " +
            "FROM revenues r GROUP BY YEAR(r.revenue_date) ORDER BY year DESC", nativeQuery = true)
    List<Map<String, Object>> findYearlyRevenue();
}