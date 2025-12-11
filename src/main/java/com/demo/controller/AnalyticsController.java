package com.demo.controller;

import com.demo.service.AnalyticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/analytics")
@CrossOrigin(origins = "*")
@PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
public class AnalyticsController {

    @Autowired
    private AnalyticsService analyticsService;

    @GetMapping("/dashboard")
    public ResponseEntity getDashboardStats() {
        return analyticsService.getDashboardStats();
    }

    @GetMapping("/revenue")
    public ResponseEntity getRevenueReport(
            @RequestParam String startDate,
            @RequestParam String endDate) {
        return analyticsService.getRevenueReport(startDate, endDate);
    }

    @GetMapping("/popular-venues")
    public ResponseEntity getPopularVenues() {
        return analyticsService.getPopularVenues();
    }

    @GetMapping("/booking-trends")
    public ResponseEntity getBookingTrends() {
        return analyticsService.getBookingTrends();
    }
}