package com.demo.service;

import com.demo.dto.BookingDTO;
import com.demo.model.Booking;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface BookingService {
    ResponseEntity createBooking(BookingDTO bookingDTO);
    ResponseEntity getAllBookings();
    ResponseEntity getBookingById(Long id);
    ResponseEntity getMyBookings(String email);
    ResponseEntity updateBookingStatus(Long id, String status);
    ResponseEntity cancelBooking(Long id);

    ResponseEntity getBookingsByOwnerId(Long ownerId);

    ResponseEntity getBookingsByVenueId(Long venueId);
}