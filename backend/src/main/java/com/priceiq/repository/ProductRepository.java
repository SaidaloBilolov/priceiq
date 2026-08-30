package com.priceiq.repository;

import com.priceiq.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByCategoryId(Long categoryId);

    List<Product> findByBrandIgnoreCase(String brand);

    @Query("SELECT p FROM Product p WHERE " +
           "LOWER(p.titleUz) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.titleRu) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.titleEn) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.brand) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.model) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<Product> searchProducts(@Param("query") String query);
}
