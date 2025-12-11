package com.demo.dao;

import com.demo.model.Booking;
import java.util.List;

public interface BookingDAO {
    void save(Booking booking);
    List<Booking> getAllBookings();
    List<Booking> getBookingById(Long id);
    List<Booking> getBookingsByUserId(int userId);
    List<Booking> getBookingsByVenueId(Long venueId);
    void delete(Booking booking);

    List<Booking> getBookingByOwnerId(Long ownerId);
}