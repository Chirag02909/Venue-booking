package com.demo.service;

import com.demo.dao.BookingDAO;
import com.demo.dao.PaymentDAO;
import com.demo.dao.UserRepository;
import com.demo.dao.VenueDAO;
import com.demo.dto.Response;
import com.demo.model.Booking;
import com.demo.model.BookingStatus;
import com.demo.model.Payment;
import com.demo.model.PaymentStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@Transactional
public class AnalyticsServiceImpl implements AnalyticsService {

    @Autowired
    private BookingDAO bookingDAO;

    @Autowired
    private PaymentDAO paymentDAO;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VenueDAO venueDAO;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public ResponseEntity getDashboardStats() {
        Response response = new Response();
        try {
            Map<String, Object> stats = new HashMap<>();

            // Total counts
            Long totalUsers = (Long) entityManager.createQuery("SELECT COUNT(u) FROM UserVO u").getSingleResult();
            Long totalVenues = (Long) entityManager.createQuery("SELECT COUNT(v) FROM VenueVO v").getSingleResult();
            Long totalBookings = (Long) entityManager.createQuery("SELECT COUNT(b) FROM Booking b").getSingleResult();
            Long totalPayments = (Long) entityManager.createQuery("SELECT COUNT(p) FROM Payment p").getSingleResult();

            stats.put("totalUsers", totalUsers);
            stats.put("totalVenues", totalVenues);
            stats.put("totalBookings", totalBookings);
            stats.put("totalPayments", totalPayments);

            // Booking status breakdown
            Map<String, Long> bookingsByStatus = new HashMap<>();
            for (BookingStatus status : BookingStatus.values()) {
                Long count = (Long) entityManager.createQuery("SELECT COUNT(b) FROM Booking b WHERE b.status = :status")
                        .setParameter("status", status)
                        .getSingleResult();
                bookingsByStatus.put(status.name(), count);
            }
            stats.put("bookingsByStatus", bookingsByStatus);

            // Revenue calculation
            Long totalRevenue = 0L;
            List<Payment> payments = paymentDAO.getAllPayments();
            if (payments != null) {
                for (Payment payment : payments) {
                    if (payment.getStatus() == PaymentStatus.COMPLETED) {
                        totalRevenue += payment.getAmount();
                    }
                }
            }
            stats.put("totalRevenue", totalRevenue);

            // This month's stats
            LocalDateTime startOfMonth = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
            Long monthlyBookings = (Long) entityManager.createQuery(
                            "SELECT COUNT(b) FROM Booking b WHERE b.startTime >= :startOfMonth")
                    .setParameter("startOfMonth", startOfMonth)
                    .getSingleResult();
            stats.put("monthlyBookings", monthlyBookings);

            response.setData(stats);
            response.setStatus(true);
            response.setMessage("Dashboard statistics retrieved successfully!");
            return new ResponseEntity(response, HttpStatus.OK);

        } catch (Exception e) {
            e.printStackTrace();
            response.setMessage("Error: " + e.getMessage());
            return new ResponseEntity(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ResponseEntity getRevenueReport(String startDateStr, String endDateStr) {
        Response response = new Response();
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE;
            LocalDate startDate = LocalDate.parse(startDateStr, formatter);
            LocalDate endDate = LocalDate.parse(endDateStr, formatter);

            LocalDateTime startDateTime = startDate.atStartOfDay();
            LocalDateTime endDateTime = endDate.atTime(23, 59, 59);

            List<Payment> payments = entityManager.createQuery(
                            "SELECT p FROM Payment p WHERE p.paymentDate BETWEEN :start AND :end", Payment.class)
                    .setParameter("start", startDateTime)
                    .setParameter("end", endDateTime)
                    .getResultList();

            Map<String, Object> revenueData = new HashMap<>();
            int totalRevenue = 0;
            int completedPayments = 0;
            int failedPayments = 0;
            int pendingPayments = 0;

            Map<String, Integer> revenueByMethod = new HashMap<>();

            for (Payment payment : payments) {
                if (payment.getStatus() == PaymentStatus.COMPLETED) {
                    totalRevenue += payment.getAmount();
                    completedPayments++;

                    String method = payment.getPaymentMethod().name();
                    revenueByMethod.put(method, revenueByMethod.getOrDefault(method, 0) + payment.getAmount());
                } else if (payment.getStatus() == PaymentStatus.FAILED) {
                    failedPayments++;
                } else if (payment.getStatus() == PaymentStatus.PENDING) {
                    pendingPayments++;
                }
            }

            revenueData.put("startDate", startDateStr);
            revenueData.put("endDate", endDateStr);
            revenueData.put("totalRevenue", totalRevenue);
            revenueData.put("completedPayments", completedPayments);
            revenueData.put("failedPayments", failedPayments);
            revenueData.put("pendingPayments", pendingPayments);
            revenueData.put("revenueByPaymentMethod", revenueByMethod);

            response.setData(revenueData);
            response.setStatus(true);
            response.setMessage("Revenue report generated successfully!");
            return new ResponseEntity(response, HttpStatus.OK);

        } catch (Exception e) {
            e.printStackTrace();
            response.setMessage("Error: " + e.getMessage());
            return new ResponseEntity(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ResponseEntity getPopularVenues() {
        Response response = new Response();
        try {
            List<Object[]> results = entityManager.createQuery(
                            "SELECT b.venueVO.id, b.venueVO.name, COUNT(b) as bookingCount " +
                                    "FROM Booking b " +
                                    "GROUP BY b.venueVO.id, b.venueVO.name " +
                                    "ORDER BY bookingCount DESC", Object[].class)
                    .setMaxResults(10)
                    .getResultList();

            List<Map<String, Object>> popularVenues = new ArrayList<>();
            for (Object[] result : results) {
                Map<String, Object> venueData = new HashMap<>();
                venueData.put("venueId", result[0]);
                venueData.put("venueName", result[1]);
                venueData.put("totalBookings", result[2]);
                popularVenues.add(venueData);
            }

            response.setData(popularVenues);
            response.setStatus(true);
            response.setMessage("Popular venues retrieved successfully!");
            return new ResponseEntity(response, HttpStatus.OK);

        } catch (Exception e) {
            e.printStackTrace();
            response.setMessage("Error: " + e.getMessage());
            return new ResponseEntity(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ResponseEntity getBookingTrends() {
        Response response = new Response();
        try {
            // Get bookings for last 12 months
            LocalDateTime twelveMonthsAgo = LocalDateTime.now().minusMonths(12);

            List<Object[]> results = entityManager.createQuery(
                            "SELECT YEAR(b.startTime), MONTH(b.startTime), COUNT(b) " +
                                    "FROM Booking b " +
                                    "WHERE b.startTime >= :startDate " +
                                    "GROUP BY YEAR(b.startTime), MONTH(b.startTime) " +
                                    "ORDER BY YEAR(b.startTime), MONTH(b.startTime)", Object[].class)
                    .setParameter("startDate", twelveMonthsAgo)
                    .getResultList();

            List<Map<String, Object>> trends = new ArrayList<>();
            for (Object[] result : results) {
                Map<String, Object> monthData = new HashMap<>();
                monthData.put("year", result[0]);
                monthData.put("month", result[1]);
                monthData.put("bookingCount", result[2]);
                trends.add(monthData);
            }

            response.setData(trends);
            response.setStatus(true);
            response.setMessage("Booking trends retrieved successfully!");
            return new ResponseEntity(response, HttpStatus.OK);

        } catch (Exception e) {
            e.printStackTrace();
            response.setMessage("Error: " + e.getMessage());
            return new ResponseEntity(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}