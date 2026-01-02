package com.demo.service;

import com.demo.config.RazorpayConfig;
import com.demo.dao.BookingDAO;
import com.demo.dao.PaymentDAO;
import com.demo.model.Booking;
import com.demo.model.BookingStatus;
import com.demo.model.Payment;
import com.demo.model.PaymentStatus;
import jakarta.transaction.Transactional;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class RazorpayWebhookHandler {

    @Autowired
    private RazorpayConfig razorpayConfig;

    @Autowired
    private PaymentDAO paymentDAO;

    @Autowired
    private BookingDAO bookingDAO;

    public boolean verifyWebhookSignature(String payload, String signature) {
        try {
            String webhookSecret = razorpayConfig.getRazorpayKeySecret();

            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(webhookSecret.getBytes(), "HmacSHA256");
            mac.init(secretKey);
            byte[] hash = mac.doFinal(payload.getBytes());

            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }

            return hexString.toString().equals(signature);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public void handlePaymentCaptured(JSONObject payload) {
        try {
            JSONObject paymentEntity = payload.getJSONObject("payload")
                    .getJSONObject("payment")
                    .getJSONObject("entity");

            String razorpayPaymentId = paymentEntity.getString("id");
            int amount = paymentEntity.getInt("amount") / 100; // Convert from paise
            String status = paymentEntity.getString("status");

            // Find payment by transaction ID
            List<Payment> payments = paymentDAO.getAllPayments();
            Payment payment = null;

            if (payments != null) {
                for (Payment p : payments) {
                    if (p.getTransactionId() != null &&
                            p.getTransactionId().equals(razorpayPaymentId)) {
                        payment = p;
                        break;
                    }
                }
            }

            if (payment != null && "captured".equals(status)) {
                payment.setStatus(PaymentStatus.COMPLETED);
                payment.setPaymentDate(LocalDateTime.now());
                payment.setPaymentGatewayResponse("Payment captured via webhook");
                paymentDAO.save(payment);

                // Update booking status
                Booking booking = payment.getBooking();
                if (booking != null && booking.getStatus() != BookingStatus.CONFIRMED) {
                    booking.setStatus(BookingStatus.CONFIRMED);
                    bookingDAO.save(booking);
                }

                System.out.println("Payment captured webhook processed: " + razorpayPaymentId);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error processing payment.captured webhook: " + e.getMessage());
        }
    }

    public void handlePaymentFailed(JSONObject payload) {
        try {
            JSONObject paymentEntity = payload.getJSONObject("payload")
                    .getJSONObject("payment")
                    .getJSONObject("entity");

            String razorpayPaymentId = paymentEntity.getString("id");
            String errorReason = paymentEntity.optString("error_reason", "Unknown error");

            // Find payment by transaction ID
            List<Payment> payments = paymentDAO.getAllPayments();
            Payment payment = null;

            if (payments != null) {
                for (Payment p : payments) {
                    if (p.getTransactionId() != null &&
                            p.getTransactionId().equals(razorpayPaymentId)) {
                        payment = p;
                        break;
                    }
                }
            }

            if (payment != null) {
                payment.setStatus(PaymentStatus.FAILED);
                payment.setPaymentGatewayResponse("Payment failed: " + errorReason);
                paymentDAO.save(payment);

                System.out.println("Payment failed webhook processed: " + razorpayPaymentId);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error processing payment.failed webhook: " + e.getMessage());
        }
    }

    public void handleRefundProcessed(JSONObject payload) {
        try {
            JSONObject refundEntity = payload.getJSONObject("payload")
                    .getJSONObject("refund")
                    .getJSONObject("entity");

            String paymentId = refundEntity.getString("payment_id");
            int refundAmount = refundEntity.getInt("amount") / 100;
            String status = refundEntity.getString("status");

            // Find payment by Razorpay payment ID
            List<Payment> payments = paymentDAO.getAllPayments();
            Payment payment = null;

            if (payments != null) {
                for (Payment p : payments) {
                    if (p.getTransactionId() != null &&
                            p.getTransactionId().equals(paymentId)) {
                        payment = p;
                        break;
                    }
                }
            }

            if (payment != null && "processed".equals(status)) {
                payment.setStatus(PaymentStatus.REFUNDED);
                payment.setPaymentGatewayResponse("Refund processed: ₹" + refundAmount);
                paymentDAO.save(payment);

                // Update booking status to cancelled
                Booking booking = payment.getBooking();
                if (booking != null) {
                    booking.setStatus(BookingStatus.CANCELLED);
                    bookingDAO.save(booking);
                }

                System.out.println("Refund processed webhook handled: " + paymentId);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error processing refund.processed webhook: " + e.getMessage());
        }
    }
}