package com.demo.dao;

import com.demo.model.Payment;
import java.util.List;

public interface PaymentDAO {
    void save(Payment payment);
    List<Payment> getPaymentsByBookingId(Long bookingId);
    List<Payment> getPaymentsByUserId(int userId);
    List<Payment> getPaymentById(Long id);
    List<Payment> getAllPayments();
}