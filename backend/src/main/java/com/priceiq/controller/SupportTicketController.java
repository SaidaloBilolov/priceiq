package com.priceiq.controller;

import com.priceiq.entity.SupportTicket;
import com.priceiq.repository.SupportTicketRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/support-tickets")
@CrossOrigin(origins = "*")
public class SupportTicketController {

    private final SupportTicketRepository supportTicketRepository;

    public SupportTicketController(SupportTicketRepository supportTicketRepository) {
        this.supportTicketRepository = supportTicketRepository;
    }

    @GetMapping
    public ResponseEntity<List<SupportTicket>> getAllTickets(@RequestParam(required = false) String status) {
        if (status != null && !status.isEmpty()) {
            return ResponseEntity.ok(supportTicketRepository.findByStatusOrderByCreatedAtDesc(status.toUpperCase()));
        }
        return ResponseEntity.ok(supportTicketRepository.findAllByOrderByCreatedAtDesc());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SupportTicket> getTicketById(@PathVariable Long id) {
        return supportTicketRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/reply")
    public ResponseEntity<SupportTicket> replyToTicket(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return supportTicketRepository.findById(id).map(ticket -> {
            ticket.setReplyText(body.get("replyText"));
            ticket.setOperatorName(body.getOrDefault("operatorName", "Admin Portal"));
            ticket.setStatus("ANSWERED");
            ticket.setRepliedAt(LocalDateTime.now());
            return ResponseEntity.ok(supportTicketRepository.save(ticket));
        }).orElse(ResponseEntity.notFound().build());
    }
}
