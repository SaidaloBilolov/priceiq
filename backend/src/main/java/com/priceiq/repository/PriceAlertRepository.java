package com.priceiq.repository;

import com.priceiq.entity.PriceAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PriceAlertRepository extends JpaRepository<PriceAlert, Long> {
    List<PriceAlert> findByUserId(Long userId);
    List<PriceAlert> findByProductIdAndIsActiveTrue(Long productId);
    void deleteByProductId(Long productId);
}
