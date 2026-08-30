package com.priceiq.repository;

import com.priceiq.entity.SupportOperator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SupportOperatorRepository extends JpaRepository<SupportOperator, Long> {

    Optional<SupportOperator> findByPhoneNumber(String phoneNumber);

    Optional<SupportOperator> findByTelegramChatId(Long telegramChatId);

    List<SupportOperator> findByIsActiveTrue();

    @Query("SELECT o FROM SupportOperator o WHERE REPLACE(REPLACE(o.phoneNumber, '+', ''), ' ', '') = :cleanPhone")
    Optional<SupportOperator> findByCleanPhone(@Param("cleanPhone") String cleanPhone);
}
