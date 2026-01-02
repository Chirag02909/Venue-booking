package com.demo.service;

import com.demo.config.RazorpayConfig;
import com.demo.dao.BookingDAO;
import com.demo.dao.PaymentDAO;
import com.demo.dao.UserRepository;
import com.demo.dto.RazorpayOrderRequest;
import com.demo.dto.RazorpayOrderResponse;
import com.demo.dto.RazorpayPaymentVerification;
import com.demo.dto.Response;
import com.demo.model.*;
import com.razorpay.Order;
import com.razorpay.Payment;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import jakarta.transaction.Transactional;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class RazorpayServiceImpl implements RazorpayService {

    @Autowired
    private RazorpayClient razorpayClient;

    @Autowired
    private RazorpayConfig razorpayConfig;

    @Autowired
    private BookingDAO bookingDAO;

    @Autowired
    private PaymentDAO paymentDAO;

    @Autowired
    private UserRepository userRepository;

    @Override
    public ResponseEntity createOrder(RazorpayOrderRequest request) {
        Response response = new Response();

        // Get authenticated user
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        UserVO user = userRepository.findByEmail(email);

        if (user == null) {
            response.setMessage("User not found!");
            return new ResponseEntity(response, HttpStatus.UNAUTHORIZED);
        }

        // Validations
        if (request.getBookingId() == null || request.getBookingId() <= 0) {
            response.setMessage("Booking ID is required!");
            return new ResponseEntity(response, HttpStatus.BAD_REQUEST);
        }

        try {
            // Get booking
            List<Booking> bookingList = bookingDAO.getBookingById(request.getBookingId());
            if (bookingList == null || bookingList.isEmpty()) {
                response.setMessage("Booking not found!");
                return new ResponseEntity(response, HttpStatus.NOT_FOUND);
            }

            Booking booking = bookingList.get(0);

            // Check if user owns this booking
            if (booking.getUserVO().getId() != user.getId()) {
                response.setMessage("You can only pay for your own bookings!");
                return new ResponseEntity(response, HttpStatus.FORBIDDEN);
            }

            // Check if booking is already paid
            List<com.demo.model.Payment> existingPayments = paymentDAO.getPaymentsByBookingId(booking.getId());
            if (existingPayments != null) {
                for (com.demo.model.Payment p : existingPayments) {
                    if (p.getStatus() == PaymentStatus.COMPLETED) {
                        response.setMessage("This booking is already paid!");
                        return new ResponseEntity(response, HttpStatus.BAD_REQUEST);
                    }
                }
            }

            // Create Razorpay order
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", booking.getTotalPrice() * 100); // Amount in paise
            orderRequest.put("currency", request.getCurrency());
            orderRequest.put("receipt", "booking_" + booking.getId());

            // Add notes
            JSONObject notes = new JSONObject();
            notes.put("booking_id", booking.getId());
            notes.put("venue_name", booking.getVenueVO().getName());
            notes.put("user_email", user.getEmail());
            if (request.getNotes() != null && !request.getNotes().isEmpty()) {
                notes.put("custom_note", request.getNotes());
            }
            orderRequest.put("notes", notes);

            Order razorpayOrder = razorpayClient.orders.create(orderRequest);

            // Create order response
            RazorpayOrderResponse orderResponse = new RazorpayOrderResponse();
            orderResponse.setOrderId(razorpayOrder.get("id"));
            orderResponse.setRazorpayKeyId(razorpayConfig.getRazorpayKeyId());
            orderResponse.setAmount(booking.getTotalPrice() * 100);
            orderResponse.setCurrency("currency", "INR");
            orderResponse.setBookingId(booking.getId());
            orderResponse.setUserName(user.getFullname());
            orderResponse.setUserEmail(user.getEmail());
            orderResponse.setUserMobile(user.getMobile());

            // Save payment record with PENDING status
            com.demo.model.Payment payment = new com.demo.model.Payment();
            payment.setBooking(booking);
            payment.setUserVO(user);
            payment.setAmount(booking.getTotalPrice());
            payment.setStatus(PaymentStatus.PENDING);
            payment.setPaymentMethod(PaymentMethod.UPI); // Will be updated after payment
            payment.setTransactionId(razorpayOrder.get("id"));
            payment.setPaymentDate(LocalDateTime.now());
            payment.setPaymentGatewayResponse("Razorpay order created: " + razorpayOrder.get("id"));

            paymentDAO.save(payment);

            response.setStatus(true);
            response.setMessage("Razorpay order created successfully!");
            response.setData(orderResponse);
            return new ResponseEntity(response, HttpStatus.OK);

        } catch (RazorpayException e) {
            e.printStackTrace();
            response.setMessage("Razorpay Error: " + e.getMessage());
            return new ResponseEntity(response, HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            e.printStackTrace();
            response.setMessage("Error: " + e.getMessage());
            return new ResponseEntity(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ResponseEntity verifyPayment(RazorpayPaymentVerification verification) {
        Response response = new Response();

        // Get authenticated user
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        UserVO user = userRepository.findByEmail(email);

        if (user == null) {
            response.setMessage("User not found!");
            return new ResponseEntity(response, HttpStatus.UNAUTHORIZED);
        }

        // Validations
        if (verification.getRazorpayOrderId() == null || verification.getRazorpayOrderId().isEmpty()) {
            response.setMessage("Order ID is required!");
            return new ResponseEntity(response, HttpStatus.BAD_REQUEST);
        }

        if (verification.getRazorpayPaymentId() == null || verification.getRazorpayPaymentId().isEmpty()) {
            response.setMessage("Payment ID is required!");
            return new ResponseEntity(response, HttpStatus.BAD_REQUEST);
        }

        if (verification.getRazorpaySignature() == null || verification.getRazorpaySignature().isEmpty()) {
            response.setMessage("Signature is required!");
            return new ResponseEntity(response, HttpStatus.BAD_REQUEST);
        }

        try {
            // Verify signature
            String generatedSignature = generateSignature(
                    verification.getRazorpayOrderId(),
                    verification.getRazorpayPaymentId()
            );

            if (!generatedSignature.equals(verification.getRazorpaySignature())) {
                response.setMessage("Invalid payment signature!");
                return new ResponseEntity(response, HttpStatus.BAD_REQUEST);
            }

            // Get payment details from Razorpay
            Payment razorpayPayment = razorpayClient.payments.fetch(verification.getRazorpayPaymentId());

            // Get booking
            List<Booking> bookingList = bookingDAO.getBookingById(verification.getBookingId());
            if (bookingList == null || bookingList.isEmpty()) {
                response.setMessage("Booking not found!");
                return new ResponseEntity(response, HttpStatus.NOT_FOUND);
            }

            Booking booking = bookingList.get(0);

            // Find existing payment record
            List<com.demo.model.Payment> paymentList = paymentDAO.getPaymentsByBookingId(booking.getId());
            com.demo.model.Payment payment = null;

            if (paymentList != null && !paymentList.isEmpty()) {
                for (com.demo.model.Payment p : paymentList) {
                    if (p.getTransactionId().equals(verification.getRazorpayOrderId())) {
                        payment = p;
                        break;
                    }
                }
            }

            if (payment == null) {
                // Create new payment record if not found
                payment = new com.demo.model.Payment();
                payment.setBooking(booking);
                payment.setUserVO(user);
                payment.setAmount(booking.getTotalPrice());
            }

            // Update payment record
            payment.setStatus(PaymentStatus.COMPLETED);
            payment.setTransactionId(verification.getRazorpayPaymentId());
            payment.setPaymentDate(LocalDateTime.now());

            // Extract payment method from Razorpay response
            String method = razorpayPayment.get("method");
            if (method != null) {
                switch (method.toLowerCase()) {
                    case "card":
                        payment.setPaymentMethod(PaymentMethod.CREDIT_CARD);
                        break;
                    case "upi":
                        payment.setPaymentMethod(PaymentMethod.UPI);
                        break;
                    case "netbanking":
                        payment.setPaymentMethod(PaymentMethod.NET_BANKING);
                        break;
                    case "wallet":
                        payment.setPaymentMethod(PaymentMethod.WALLET);
                        break;
                    default:
                        payment.setPaymentMethod(PaymentMethod.UPI);
                }
            }

            payment.setPaymentGatewayResponse("Payment verified. Razorpay Payment ID: " + verification.getRazorpayPaymentId());

            paymentDAO.save(payment);

            // Update booking status to CONFIRMED
            booking.setStatus(BookingStatus.CONFIRMED);
            bookingDAO.save(booking);

            // Prepare response data
            Map<String, Object> paymentData = new HashMap<>();
            paymentData.put("paymentId", payment.getId());
            paymentData.put("razorpayPaymentId", verification.getRazorpayPaymentId());
            paymentData.put("razorpayOrderId", verification.getRazorpayOrderId());
            paymentData.put("bookingId", booking.getId());
            paymentData.put("amount", payment.getAmount());
            paymentData.put("status", "COMPLETED");
            paymentData.put("bookingStatus", "CONFIRMED");

            response.setStatus(true);
            response.setMessage("Payment verified and booking confirmed successfully!");
            response.setData(paymentData);
            return new ResponseEntity(response, HttpStatus.OK);

        } catch (RazorpayException e) {
            e.printStackTrace();
            response.setMessage("Razorpay Error: " + e.getMessage());
            return new ResponseEntity(response, HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            e.printStackTrace();
            response.setMessage("Error: " + e.getMessage());
            return new ResponseEntity(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ResponseEntity getPaymentDetails(String paymentId) {
        Response response = new Response();

        if (paymentId == null || paymentId.isEmpty()) {
            response.setMessage("Payment ID is required!");
            return new ResponseEntity(response, HttpStatus.BAD_REQUEST);
        }

        try {
            Payment razorpayPayment = razorpayClient.payments.fetch(paymentId);

            Map<String, Object> paymentDetails = new HashMap<>();
            paymentDetails.put("id", razorpayPayment.get("id"));
            paymentDetails.put("amount", razorpayPayment.get("amount"));
            paymentDetails.put("currency", razorpayPayment.get("currency"));
            paymentDetails.put("status", razorpayPayment.get("status"));
            paymentDetails.put("method", razorpayPayment.get("method"));
            paymentDetails.put("email", razorpayPayment.get("email"));
            paymentDetails.put("contact", razorpayPayment.get("contact"));
            paymentDetails.put("createdAt", razorpayPayment.get("created_at"));

            response.setStatus(true);
            response.setMessage("Payment details retrieved successfully!");
            response.setData(paymentDetails);
            return new ResponseEntity(response, HttpStatus.OK);

        } catch (RazorpayException e) {
            e.printStackTrace();
            response.setMessage("Razorpay Error: " + e.getMessage());
            return new ResponseEntity(response, HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            e.printStackTrace();
            response.setMessage("Error: " + e.getMessage());
            return new ResponseEntity(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Helper method to generate signature for verification
    private String generateSignature(String orderId, String paymentId) throws Exception {
        String payload = orderId + "|" + paymentId;
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKey = new SecretKeySpec(razorpayConfig.getRazorpayKeySecret().getBytes(), "HmacSHA256");
        mac.init(secretKey);
        byte[] hash = mac.doFinal(payload.getBytes());

        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }
}