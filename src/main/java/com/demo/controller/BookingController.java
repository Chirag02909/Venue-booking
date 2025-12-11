package com.demo.controller;

import com.demo.dto.BookingDTO;
import com.demo.model.Booking;
import com.demo.service.BookingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/bookings")
@CrossOrigin(origins = "*")
public class BookingController {

    @Autowired
    private BookingService bookingService;

    @PostMapping
    public ResponseEntity createBooking(@RequestBody BookingDTO bookingDTO) {
        return bookingService.createBooking(bookingDTO);
    }

    @GetMapping
    public ResponseEntity getMyBookings() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        return bookingService.getMyBookings(email);
    }

    @GetMapping("/{id}")
    public ResponseEntity getBookingById(@PathVariable Long id) {
        return bookingService.getBookingById(id);
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity cancelBooking(@PathVariable Long id) {
        return bookingService.cancelBooking(id);
    }

    @GetMapping("/owner/{ownerId}")
    public ResponseEntity getBookingsByOwnerId(@PathVariable Long ownerId) {
        return bookingService.getBookingsByOwnerId(ownerId);
    }


    @GetMapping("/all")
//    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity getAllBookings() {
        return bookingService.getAllBookings();
    }

    @PutMapping("/{id}")
//    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity updateBookingStatus(@PathVariable Long id, @RequestParam String status) {
        return bookingService.updateBookingStatus(id, status);
    }

//    @GetMapping("/owner/{ownerId}")
//    public ResponseEntity<?> getBookingsForOwner(@PathVariable Long ownerId) {
//        List<Booking> bookings = bookingRepository.findBookingsByOwnerId(ownerId);
//        return ResponseEntity.ok(bookings);
//    }

    @GetMapping("/venue/{venueId}/booked-dates")
    public ResponseEntity<?> getBookedDates(@PathVariable Long venueId) {
        return bookingService.getBookingsByVenueId(venueId);
    }

}