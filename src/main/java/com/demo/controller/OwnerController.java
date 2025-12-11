package com.demo.controller;

import com.demo.dto.VenueDTO;
import com.demo.service.BookingService;
import com.demo.service.VenueService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/owner")
@CrossOrigin(origins = "*")
public class OwnerController {

    @Autowired
    private VenueService venueService;

    @Autowired
    private BookingService bookingService;

    @PostMapping("/venues")
    public ResponseEntity createVenue(@RequestBody VenueDTO venueDTO) {
        return venueService.createVenue(venueDTO);
    }

    @DeleteMapping("/venues/{id}")
    public ResponseEntity deleteVenue(@PathVariable int id) {
        return venueService.deleteVenue(id);
    }

    @PutMapping("/venues/{id}")
    public ResponseEntity updateVenue(@PathVariable int id , @RequestBody VenueDTO venueDTO) {
        return venueService.updateVenue(id,venueDTO);
    }

    @GetMapping("/bookings/{id}")
    public ResponseEntity getBookingById(@PathVariable Long id) {
        return bookingService.getBookingById(id);
    }
//    public ResponseEntity getAllBookings() {
//        return bookingService.getAllBookings();
//    }

//    @GetMapping
//    public ResponseEntity getMyBookings() {
//        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
//        String email = auth.getName();
//        return bookingService.getMyBookings(email);
//    }

    // Update booking status
//    @PutMapping("/bookings/{bookingId}/status")
//    public ResponseEntity<?> updateBookingStatus(
//            @PathVariable Long bookingId,
//            @RequestBody Map<String, String> request) {
//        String status = request.get("status");
//        bookingService.updateBookingStatus(bookingId, status);
//        return ResponseEntity.ok().build();
//    }
}