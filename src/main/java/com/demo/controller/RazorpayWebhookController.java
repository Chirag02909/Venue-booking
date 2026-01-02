package com.demo.controller;

import com.demo.service.RazorpayWebhookHandler;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/razorpay")
@CrossOrigin(origins = "*")
public class RazorpayWebhookController {

    @Autowired
    private RazorpayWebhookHandler webhookHandler;

    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature) {

        System.out.println("Webhook received");

        // Verify webhook signature
        if (signature == null || signature.isEmpty()) {
            System.err.println("Missing webhook signature");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Missing signature");
        }

        if (!webhookHandler.verifyWebhookSignature(payload, signature)) {
            System.err.println("Invalid webhook signature");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid signature");
        }

        try {
            JSONObject webhookPayload = new JSONObject(payload);
            String event = webhookPayload.getString("event");

            System.out.println("Processing webhook event: " + event);

            switch (event) {
                case "payment.captured":
                    webhookHandler.handlePaymentCaptured(webhookPayload);
                    break;

                case "payment.failed":
                    webhookHandler.handlePaymentFailed(webhookPayload);
                    break;

                case "refund.processed":
                    webhookHandler.handleRefundProcessed(webhookPayload);
                    break;

                default:
                    System.out.println("Unhandled webhook event: " + event);
            }

            return ResponseEntity.ok("Webhook processed successfully");

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error processing webhook: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing webhook");
        }
    }
}