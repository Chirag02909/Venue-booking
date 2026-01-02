package com.demo.service;

import com.demo.dao.BookingDAO;
import com.demo.dao.PaymentDAO;
import com.demo.dao.UserRepository;
import com.demo.dto.RefundRequest;
import com.demo.dto.Response;
import com.demo.model.*;
import com.razorpay.Refund;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class RazorpayRefundServiceImpl implements RazorpayRefundService {

    @Autowired
    private RazorpayClient razorpayClient;

    @Autowired
    private PaymentDAO paymentDAO;

    @Autowired
    private BookingDAO bookingDAO;

    @Autowired
    private UserRepository userRepository;

    @Override
    public ResponseEntity initiateRefund(RefundRequest request) {
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
        if (request.getPaymentId() == null || request.getPaymentId() <= 0) {
            response.setMessage("Payment ID is required!");
            return new ResponseEntity(response, HttpStatus.BAD_REQUEST);
        }

        try {
            // Get payment record
            List<Payment> paymentList = paymentDAO.getPaymentById(request.getPaymentId());
            if (paymentList == null || paymentList.isEmpty()) {
                response.setMessage("Payment not found!");
                return new ResponseEntity(response, HttpStatus.NOT_FOUND);
            }

            Payment payment = paymentList.get(0);

            // Check if payment is completed
            if (payment.getStatus() != PaymentStatus.COMPLETED) {
                response.setMessage("Only completed payments can be refunded!");
                return new ResponseEntity(response, HttpStatus.BAD_REQUEST);
            }

            // Check if already refunded
            if (payment.getStatus() == PaymentStatus.REFUNDED) {
                response.setMessage("Payment already refunded!");
                return new ResponseEntity(response, HttpStatus.BAD_REQUEST);
            }

            // Check user authorization (only admin or payment owner)
            boolean isAdmin = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

            if (!isAdmin && payment.getUserVO().getId() != user.getId()) {
                response.setMessage("You can only request refund for your own payments!");
                return new ResponseEntity(response, HttpStatus.FORBIDDEN);
            }

            // Determine refund amount
            int refundAmount = request.getAmount() != null && request.getAmount() > 0
                    ? request.getAmount()
                    : payment.getAmount();

            if (refundAmount > payment.getAmount()) {
                response.setMessage("Refund amount cannot exceed payment amount!");
                return new ResponseEntity(response, HttpStatus.BAD_REQUEST);
            }

            // Create refund request
            JSONObject refundRequest = new JSONObject();
            refundRequest.put("amount", refundAmount * 100); // Amount in paise

            if (request.getReason() != null && !request.getReason().isEmpty()) {
                JSONObject notes = new JSONObject();
                notes.put("reason", request.getReason());
                refundRequest.put("notes", notes);
            }

            // Initiate refund with Razorpay
            Refund razorpayRefund = razorpayClient.payments.refund(
                    payment.getTransactionId(),
                    refundRequest
            );

            // Update payment status
            payment.setStatus(PaymentStatus.REFUNDED);
            payment.setPaymentGatewayResponse(
                    "Refund initiated: " + razorpayRefund.get("id") +
                            " | Amount: ₹" + refundAmount
            );
            paymentDAO.save(payment);

            // Update booking status
            Booking booking = payment.getBooking();
            if (booking != null) {
                booking.setStatus(BookingStatus.CANCELLED);
                bookingDAO.save(booking);
            }

            // Prepare response
            Map<String, Object> refundData = new HashMap<>();
            refundData.put("refundId", razorpayRefund.get("id"));
            refundData.put("paymentId", payment.getId());
            refundData.put("razorpayPaymentId", payment.getTransactionId());
            refundData.put("amount", refundAmount);
            refundData.put("status", razorpayRefund.get("status"));
            refundData.put("bookingId", booking != null ? booking.getId() : null);

            response.setStatus(true);
            response.setMessage("Refund initiated successfully!");
            response.setData(refundData);
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
    public ResponseEntity getRefundStatus(String refundId) {
        Response response = new Response();

        if (refundId == null || refundId.isEmpty()) {
            response.setMessage("Refund ID is required!");
            return new ResponseEntity(response, HttpStatus.BAD_REQUEST);
        }

        try {
            Refund refund = razorpayClient.refunds.fetch(refundId);

            Map<String, Object> refundDetails = new HashMap<>();
            refundDetails.put("id", refund.get("id"));
            refundDetails.put("paymentId", refund.get("payment_id"));
            refundDetails.put("amount", refund.get("amount"));
            refundDetails.put("currency", refund.get("currency"));
            refundDetails.put("status", refund.get("status"));
            refundDetails.put("createdAt", refund.get("created_at"));

            response.setStatus(true);
            response.setMessage("Refund details retrieved successfully!");
            response.setData(refundDetails);
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

    @Override
    public ResponseEntity getAllRefunds(String paymentId) {
        Response response = new Response();

        if (paymentId == null || paymentId.isEmpty()) {
            response.setMessage("Payment ID is required!");
            return new ResponseEntity(response, HttpStatus.BAD_REQUEST);
        }

        try {
            List<Refund> refunds = razorpayClient.payments.fetchAllRefunds(paymentId);

            response.setStatus(true);
            response.setMessage("Refunds retrieved successfully!");
            response.setData(refunds);
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
}