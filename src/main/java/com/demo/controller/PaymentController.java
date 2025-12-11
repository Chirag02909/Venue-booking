package com.demo.controller;

import com.demo.dto.PaymentDTO;
import com.demo.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@CrossOrigin(origins = "*")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @PostMapping
    public ResponseEntity createPayment(@RequestBody PaymentDTO paymentDTO) {
        return paymentService.createPayment(paymentDTO);
    }

    @GetMapping
    public ResponseEntity getMyPayments() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        return paymentService.getMyPayments(email);
    }

    @GetMapping("/booking/{bookingId}")
    public ResponseEntity getPaymentsByBooking(@PathVariable Long bookingId) {
        return paymentService.getPaymentsByBooking(bookingId);
    }

    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity getAllPayments() {
        return paymentService.getAllPayments();
    }

    @PutMapping("/{paymentId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity updatePaymentStatus(@PathVariable Long paymentId, @RequestParam String status) {
        return paymentService.updatePaymentStatus(paymentId, status);
    }
}