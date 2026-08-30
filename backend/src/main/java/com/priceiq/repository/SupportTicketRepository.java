package com.priceiq.repository;

import com.priceiq.entity.SupportTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> {

    List<SupportTicket> findAllByOrderByCreatedAtDesc();

    List<SupportTicket> findByStatusOrderByCreatedAtDesc(String status);

    Optional<SupportTicket> findTopByUserChatIdAndStatusOrderByCreatedAtDesc(Long userChatId, String status);

    List<SupportTicket> findByUserChatIdOrderByCreatedAtDesc(Long userChatId);
}
