package com.demo.service;

import com.demo.dto.RazorpayOrderRequest;
import com.demo.dto.RazorpayPaymentVerification;
import org.springframework.http.ResponseEntity;

public interface RazorpayService {
    ResponseEntity createOrder(RazorpayOrderRequest request);
    ResponseEntity verifyPayment(RazorpayPaymentVerification verification);
    ResponseEntity getPaymentDetails(String paymentId);
}