package com.demo.service;

import com.demo.dao.BookingDAO;
import com.demo.dao.PaymentDAO;
import com.demo.dao.UserRepository;
import com.demo.dto.PaymentDTO;
import com.demo.dto.Response;
import com.demo.model.*;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class PaymentServiceImpl implements PaymentService {

    @Autowired
    private PaymentDAO paymentDAO;

    @Autowired
    private BookingDAO bookingDAO;

    @Autowired
    private UserRepository userRepository;

    @Override
    public ResponseEntity createPayment(PaymentDTO paymentDTO) {
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
        if (paymentDTO.getBookingId() == null || paymentDTO.getBookingId() <= 0) {
            response.setMessage("Booking ID is required!");
            return new ResponseEntity(response, HttpStatus.BAD_REQUEST);
        }

        if (paymentDTO.getAmount() <= 0) {
            response.setMessage("Amount must be greater than 0!");
            return new ResponseEntity(response, HttpStatus.BAD_REQUEST);
        }

        if (paymentDTO.getPaymentMethod() == null) {
            response.setMessage("Payment method is required!");
            return new ResponseEntity(response, HttpStatus.BAD_REQUEST);
        }

        try {
            // Get booking
            List<Booking> bookingList = bookingDAO.getBookingById(paymentDTO.getBookingId());
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

            // Create payment
            Payment payment = new Payment();
            payment.setBooking(booking);
            payment.setUserVO(user);
            payment.setAmount(paymentDTO.getAmount());
            payment.setPaymentMethod(paymentDTO.getPaymentMethod());
            payment.setPaymentDate(LocalDateTime.now());

            // Generate transaction ID
            String transactionId = "TXN" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            payment.setTransactionId(transactionId);

            // Simulate payment processing
            if (paymentDTO.getAmount() == booking.getTotalPrice()) {
                payment.setStatus(PaymentStatus.COMPLETED);
                payment.setPaymentGatewayResponse("Payment successful");

                // Update booking status to CONFIRMED
                booking.setStatus(BookingStatus.CONFIRMED);
                bookingDAO.save(booking);
            } else {
                payment.setStatus(PaymentStatus.FAILED);
                payment.setPaymentGatewayResponse("Amount mismatch");
            }

            paymentDAO.save(payment);

            response.setStatus(true);
            response.setMessage("Payment processed successfully!");
            response.setData(payment);
            return new ResponseEntity(response, HttpStatus.OK);

        } catch (Exception e) {
            e.printStackTrace();
            response.setMessage("Error: " + e.getMessage());
            return new ResponseEntity(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ResponseEntity getPaymentsByBooking(Long bookingId) {
        Response response = new Response();
        try {
            List<Payment> payments = paymentDAO.getPaymentsByBookingId(bookingId);

            response.setData(payments);
            response.setStatus(true);
            response.setMessage(payments == null || payments.isEmpty() ? "No payments found" : "");
            return new ResponseEntity(response, HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            response.setMessage("Error: " + e.getMessage());
            return new ResponseEntity(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ResponseEntity getMyPayments(String email) {
        Response response = new Response();
        try {
            UserVO user = userRepository.findByEmail(email);
            if (user == null) {
                response.setMessage("User not found!");
                return new ResponseEntity(response, HttpStatus.NOT_FOUND);
            }

            List<Payment> payments = paymentDAO.getPaymentsByUserId(user.getId());

            response.setData(payments);
            response.setStatus(true);
            response.setMessage(payments == null || payments.isEmpty() ? "No payments found" : "");
            return new ResponseEntity(response, HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            response.setMessage("Error: " + e.getMessage());
            return new ResponseEntity(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ResponseEntity getAllPayments() {
        Response response = new Response();
        try {
            List<Payment> payments = paymentDAO.getAllPayments();

            response.setData(payments);
            response.setStatus(true);
            response.setMessage(payments == null || payments.isEmpty() ? "No payments found" : "");
            return new ResponseEntity(response, HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            response.setMessage("Error: " + e.getMessage());
            return new ResponseEntity(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ResponseEntity updatePaymentStatus(Long paymentId, String status) {
        Response response = new Response();
        try {
            List<Payment> paymentList = paymentDAO.getPaymentById(paymentId);

            if (paymentList == null || paymentList.isEmpty()) {
                response.setMessage("Payment not found!");
                return new ResponseEntity(response, HttpStatus.NOT_FOUND);
            }

            Payment payment = paymentList.get(0);

            try {
                PaymentStatus newStatus = PaymentStatus.valueOf(status.toUpperCase());
                payment.setStatus(newStatus);
                paymentDAO.save(payment);

                response.setStatus(true);
                response.setMessage("Payment status updated successfully!");
                response.setData(payment);
                return new ResponseEntity(response, HttpStatus.OK);
            } catch (IllegalArgumentException e) {
                response.setMessage("Invalid status value! Use: PENDING, COMPLETED, FAILED, or REFUNDED");
                return new ResponseEntity(response, HttpStatus.BAD_REQUEST);
            }

        } catch (Exception e) {
            e.printStackTrace();
            response.setMessage("Error: " + e.getMessage());
            return new ResponseEntity(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
