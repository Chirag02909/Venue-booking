package com.demo.controller;

import com.demo.dto.RazorpayOrderRequest;
import com.demo.dto.RazorpayPaymentVerification;
import com.demo.service.RazorpayService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/razorpay")
@CrossOrigin(origins = "*")
public class RazorpayController {

    @Autowired
    private RazorpayService razorpayService;

    @PostMapping("/create-order")
    public ResponseEntity createOrder(@RequestBody RazorpayOrderRequest request) {
        return razorpayService.createOrder(request);
    }

    @PostMapping("/verify-payment")
    public ResponseEntity verifyPayment(@RequestBody RazorpayPaymentVerification verification) {
        return razorpayService.verifyPayment(verification);
    }

    @GetMapping("/payment-details/{paymentId}")
    public ResponseEntity getPaymentDetails(@PathVariable String paymentId) {
        return razorpayService.getPaymentDetails(paymentId);
    }
}