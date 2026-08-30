package com.priceiq.repository;

import com.priceiq.entity.ProductOffer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductOfferRepository extends JpaRepository<ProductOffer, Long> {
    List<ProductOffer> findByProductIdOrderByPriceUzsAsc(Long productId);
    List<ProductOffer> findByStoreId(Long storeId);
    List<ProductOffer> findByProductId(Long productId);
}
