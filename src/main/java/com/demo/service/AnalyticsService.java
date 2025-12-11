package com.demo.service;

import org.springframework.http.ResponseEntity;

public interface AnalyticsService {
    ResponseEntity getDashboardStats();
    ResponseEntity getRevenueReport(String startDate, String endDate);
    ResponseEntity getPopularVenues();
    ResponseEntity getBookingTrends();
}