package com.demo.service;

import com.demo.dto.RefundRequest;
import org.springframework.http.ResponseEntity;

public interface RazorpayRefundService {
    ResponseEntity initiateRefund(RefundRequest request);
    ResponseEntity getRefundStatus(String refundId);
    ResponseEntity getAllRefunds(String paymentId);
}