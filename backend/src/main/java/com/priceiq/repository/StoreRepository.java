package com.priceiq.repository;

import com.priceiq.entity.Store;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StoreRepository extends JpaRepository<Store, Long> {
    Optional<Store> findByOwnerPhone(String ownerPhone);
    Optional<Store> findByOwnerChatId(Long ownerChatId);
    Optional<Store> findByNameIgnoreCase(String name);

    @Query("SELECT s FROM Store s WHERE REPLACE(REPLACE(s.ownerPhone, ' ', ''), '+', '') = :cleanPhone")
    Optional<Store> findByCleanPhone(@Param("cleanPhone") String cleanPhone);
}
