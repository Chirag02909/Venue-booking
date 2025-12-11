package com.demo.service;

import com.demo.dto.PaymentDTO;
import org.springframework.http.ResponseEntity;

public interface PaymentService {
    ResponseEntity createPayment(PaymentDTO paymentDTO);
    ResponseEntity getPaymentsByBooking(Long bookingId);
    ResponseEntity getMyPayments(String email);
    ResponseEntity getAllPayments();
    ResponseEntity updatePaymentStatus(Long paymentId, String status);
}