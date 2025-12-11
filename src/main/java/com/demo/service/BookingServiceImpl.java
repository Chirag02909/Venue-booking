package com.demo.service;

import com.demo.dao.BookingDAO;
import com.demo.dao.UserRepository;
import com.demo.dao.VenueDAO;
import com.demo.dto.BookingDTO;
import com.demo.dto.Response;
import com.demo.model.Booking;
import com.demo.model.BookingStatus;
import com.demo.model.UserVO;
import com.demo.model.VenueVO;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class BookingServiceImpl implements BookingService {

    @Autowired
    private BookingDAO bookingDAO;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VenueDAO venueDAO;

    @Override
    public ResponseEntity createBooking(BookingDTO bookingDTO) {
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
        if (bookingDTO.getVenueVO() == null || bookingDTO.getVenueVO().getId() <= 0) {
            response.setMessage("Venue is required!");
            return new ResponseEntity(response, HttpStatus.BAD_REQUEST);
        }

        if (bookingDTO.getStartTime() == null) {
            response.setMessage("Start time is required!");
            return new ResponseEntity(response, HttpStatus.BAD_REQUEST);
        }

        if (bookingDTO.getEndTime() == null) {
            response.setMessage("End time is required!");
            return new ResponseEntity(response, HttpStatus.BAD_REQUEST);
        }

        if (bookingDTO.getStartTime().isAfter(bookingDTO.getEndTime())) {
            response.setMessage("End time must be after start time!");
            return new ResponseEntity(response, HttpStatus.BAD_REQUEST);
        }

        if (bookingDTO.getStartTime().isBefore(LocalDateTime.now())) {
            response.setMessage("Start time cannot be in the past!");
            return new ResponseEntity(response, HttpStatus.BAD_REQUEST);
        }

        try {
            // Get venue
            List<VenueVO> venueList = venueDAO.getVenueById(bookingDTO.getVenueVO().getId());
            if (venueList == null || venueList.isEmpty()) {
                response.setMessage("Venue not found!");
                return new ResponseEntity(response, HttpStatus.NOT_FOUND);
            }
            VenueVO venue = venueList.get(0);

            // Calculate total price (days * pricePerDay)
            long days = ChronoUnit.DAYS.between(bookingDTO.getStartTime().toLocalDate(),
                    bookingDTO.getEndTime().toLocalDate()) + 1;
            int totalPrice = (int) days * venue.getPricePerDay();

            // Create booking
            Booking booking = new Booking();
            booking.setUserVO(user);
            booking.setVenueVO(venue);
            booking.setStartTime(bookingDTO.getStartTime());
            booking.setEndTime(bookingDTO.getEndTime());
            booking.setTotalPrice(totalPrice);
            booking.setStatus(BookingStatus.PENDING);
            booking.setEventType(bookingDTO.getEventType());
            booking.setSpecialRequests(bookingDTO.getSpecialRequests());

            bookingDAO.save(booking);

            response.setStatus(true);
            response.setMessage("Booking created successfully!");
            response.setData(booking);
            return new ResponseEntity(response, HttpStatus.OK);

        } catch (Exception e) {
            e.printStackTrace();
            response.setMessage("Error: " + e.getMessage());
            return new ResponseEntity(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ResponseEntity getAllBookings() {
        Response response = new Response();
        try {
            List<Booking> bookings = bookingDAO.getAllBookings();

            response.setData(bookings);
            response.setStatus(true);
            response.setMessage(bookings == null || bookings.isEmpty() ? "No bookings found" : "");
            return new ResponseEntity(response, HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            response.setMessage("Error: " + e.getMessage());
            return new ResponseEntity(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ResponseEntity getBookingById(Long id) {
        Response response = new Response();
        try {
            List<Booking> bookings = bookingDAO.getBookingById(id);

            if (bookings == null || bookings.isEmpty()) {
                response.setMessage("Booking not found!");
                return new ResponseEntity(response, HttpStatus.NOT_FOUND);
            }

            response.setData(bookings.get(0));
            response.setStatus(true);
            return new ResponseEntity(response, HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            response.setMessage("Error: " + e.getMessage());
            return new ResponseEntity(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    @Override
    public ResponseEntity getBookingsByOwnerId(Long ownerId) {
        Response response = new Response();
        try {
            List<Booking> bookings = bookingDAO.getBookingByOwnerId(ownerId);

            if (bookings == null || bookings.isEmpty()) {
                response.setMessage("Booking not found!");
                return new ResponseEntity(response, HttpStatus.NOT_FOUND);
            }

            response.setData(bookings);
            response.setStatus(true);
            return new ResponseEntity(response, HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            response.setMessage("Error: " + e.getMessage());
            return new ResponseEntity(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ResponseEntity getMyBookings(String email) {
        Response response = new Response();
        try {
            UserVO user = userRepository.findByEmail(email);
            if (user == null) {
                response.setMessage("User not found!");
                return new ResponseEntity(response, HttpStatus.NOT_FOUND);
            }

            List<Booking> bookings = bookingDAO.getBookingsByUserId(user.getId());

            response.setData(bookings);
            response.setStatus(true);
            response.setMessage(bookings == null || bookings.isEmpty() ? "No bookings found" : "");
            return new ResponseEntity(response, HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            response.setMessage("Error: " + e.getMessage());
            return new ResponseEntity(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ResponseEntity updateBookingStatus(Long id, String status) {
        Response response = new Response();
        try {
            List<Booking> bookingList = bookingDAO.getBookingById(id);

            if (bookingList == null || bookingList.isEmpty()) {
                response.setMessage("Booking not found!");
                return new ResponseEntity(response, HttpStatus.NOT_FOUND);
            }

            Booking booking = bookingList.get(0);

            try {
                BookingStatus newStatus = BookingStatus.valueOf(status.toUpperCase());
                booking.setStatus(newStatus);
                bookingDAO.save(booking);

                response.setStatus(true);
                response.setMessage("Booking status updated successfully!");
                response.setData(booking);
                return new ResponseEntity(response, HttpStatus.OK);
            } catch (IllegalArgumentException e) {
                response.setMessage("Invalid status value! Use: PENDING, CONFIRMED, CANCELLED, or COMPLETED");
                return new ResponseEntity(response, HttpStatus.BAD_REQUEST);
            }

        } catch (Exception e) {
            e.printStackTrace();
            response.setMessage("Error: " + e.getMessage());
            return new ResponseEntity(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ResponseEntity cancelBooking(Long id) {
        Response response = new Response();
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String email = auth.getName();
            UserVO user = userRepository.findByEmail(email);

            List<Booking> bookingList = bookingDAO.getBookingById(id);

            if (bookingList == null || bookingList.isEmpty()) {
                response.setMessage("Booking not found!");
                return new ResponseEntity(response, HttpStatus.NOT_FOUND);
            }

            Booking booking = bookingList.get(0);

            // Check if user owns this booking
            if (booking.getUserVO().getId() != user.getId()) {
                response.setMessage("You can only cancel your own bookings!");
                return new ResponseEntity(response, HttpStatus.FORBIDDEN);
            }

            if (booking.getStatus() == BookingStatus.CANCELLED) {
                response.setMessage("Booking is already cancelled!");
                return new ResponseEntity(response, HttpStatus.BAD_REQUEST);
            }

            booking.setStatus(BookingStatus.CANCELLED);
            bookingDAO.save(booking);

            response.setStatus(true);
            response.setMessage("Booking cancelled successfully!");
            response.setData(booking);
            return new ResponseEntity(response, HttpStatus.OK);

        } catch (Exception e) {
            e.printStackTrace();
            response.setMessage("Error: " + e.getMessage());
            return new ResponseEntity(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ResponseEntity getBookingsByVenueId(Long venueId) {
        Response response = new Response();
        try {
            List<Booking> bookings = bookingDAO.getBookingsByVenueId(venueId);

            if (bookings == null || bookings.isEmpty()) {
                response.setMessage("No bookings found for this venue!");
                response.setStatus(true);
                response.setData(List.of());
                return new ResponseEntity(response, HttpStatus.OK);
            }

            // Filter out CANCELLED bookings and map to date ranges
            List<Map<String, String>> bookedDates = bookings.stream()
                    .filter(b -> b.getStatus() != null && b.getStatus() != BookingStatus.CANCELLED)
                    .map(b -> Map.of(
                            "start", b.getStartTime().toString(),
                            "end", b.getEndTime().toString()
                    ))
                    .collect(Collectors.toList());

            response.setStatus(true);
            response.setMessage("Booked dates fetched successfully!");
            response.setData(bookedDates);
            return new ResponseEntity(response, HttpStatus.OK);

        } catch (Exception e) {
            e.printStackTrace();
            response.setMessage("Error fetching booked dates: " + e.getMessage());
            return new ResponseEntity(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}