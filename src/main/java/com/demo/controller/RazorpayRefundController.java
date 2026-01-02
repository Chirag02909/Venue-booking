package com.demo.controller;

import com.demo.dto.RefundRequest;
import com.demo.service.RazorpayRefundService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/razorpay/refunds")
@CrossOrigin(origins = "*")
public class RazorpayRefundController {

    @Autowired
    private RazorpayRefundService refundService;

    @PostMapping("/initiate")
    public ResponseEntity initiateRefund(@RequestBody RefundRequest request) {
        return refundService.initiateRefund(request);
    }

    @GetMapping("/{refundId}")
    public ResponseEntity getRefundStatus(@PathVariable String refundId) {
        return refundService.getRefundStatus(refundId);
    }

    @GetMapping("/payment/{paymentId}")
    public ResponseEntity getAllRefunds(@PathVariable String paymentId) {
        return refundService.getAllRefunds(paymentId);
    }
}