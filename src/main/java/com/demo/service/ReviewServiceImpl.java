package com.demo.service;

import com.demo.dao.BookingDAO;
import com.demo.dao.ReviewDAO;
import com.demo.dao.UserRepository;
import com.demo.dao.VenueDAO;
import com.demo.dto.Response;
import com.demo.dto.ReviewDTO;
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

@Service
@Transactional
public class ReviewServiceImpl implements ReviewService {

    @Autowired
    private ReviewDAO reviewDAO;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VenueDAO venueDAO;

    @Autowired
    private BookingDAO bookingDAO;

    @Override
    public ResponseEntity createReview(ReviewDTO reviewDTO) {
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
        if (reviewDTO.getVenueId() == null || reviewDTO.getVenueId() <= 0) {
            response.setMessage("Venue ID is required!");
            return new ResponseEntity(response, HttpStatus.BAD_REQUEST);
        }

        if (reviewDTO.getRating() < 1 || reviewDTO.getRating() > 5) {
            response.setMessage("Rating must be between 1 and 5!");
            return new ResponseEntity(response, HttpStatus.BAD_REQUEST);
        }

        if (reviewDTO.getComment() == null || reviewDTO.getComment().trim().isEmpty()) {
            response.setMessage("Comment is required!");
            return new ResponseEntity(response, HttpStatus.BAD_REQUEST);
        }

        try {
            // Get venue
            List<VenueVO> venueList = venueDAO.getVenueById(reviewDTO.getVenueId().intValue());
            if (venueList == null || venueList.isEmpty()) {
                response.setMessage("Venue not found!");
                return new ResponseEntity(response, HttpStatus.NOT_FOUND);
            }
            VenueVO venue = venueList.get(0);

            // Create review
            Review review = new Review();
            review.setUserVO(user);
            review.setVenueVO(venue);
            review.setRating(reviewDTO.getRating());
            review.setComment(reviewDTO.getComment());
            review.setCreatedAt(LocalDateTime.now());

            // If booking ID provided, link it
            if (reviewDTO.getBookingId() != null && reviewDTO.getBookingId() > 0) {
                List<Booking> bookingList = bookingDAO.getBookingById(reviewDTO.getBookingId());
                if (bookingList != null && !bookingList.isEmpty()) {
                    Booking booking = bookingList.get(0);

                    // Verify user owns the booking
                    if (booking.getUserVO().getId() == user.getId()) {
                        review.setBooking(booking);
                    }
                }
            }

            reviewDAO.save(review);

            response.setStatus(true);
            response.setMessage("Review submitted successfully!");
            response.setData(review);
            return new ResponseEntity(response, HttpStatus.OK);

        } catch (Exception e) {
            e.printStackTrace();
            response.setMessage("Error: " + e.getMessage());
            return new ResponseEntity(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ResponseEntity getReviewsByVenue(int venueId) {
        Response response = new Response();
        try {
            List<Review> reviews = reviewDAO.getReviewsByVenueId(venueId);
            Double avgRating = reviewDAO.getAverageRatingByVenueId(venueId);

            response.setData(reviews);
            response.setStatus(true);
            response.setMessage(reviews == null || reviews.isEmpty() ? "No reviews found" :
                    String.format("Average Rating: %.1f", avgRating));
            return new ResponseEntity(response, HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            response.setMessage("Error: " + e.getMessage());
            return new ResponseEntity(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ResponseEntity getMyReviews(String email) {
        Response response = new Response();
        try {
            UserVO user = userRepository.findByEmail(email);
            if (user == null) {
                response.setMessage("User not found!");
                return new ResponseEntity(response, HttpStatus.NOT_FOUND);
            }

            List<Review> reviews = reviewDAO.getReviewsByUserId(user.getId());

            response.setData(reviews);
            response.setStatus(true);
            response.setMessage(reviews == null || reviews.isEmpty() ? "No reviews found" : "");
            return new ResponseEntity(response, HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            response.setMessage("Error: " + e.getMessage());
            return new ResponseEntity(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ResponseEntity deleteReview(Long reviewId) {
        Response response = new Response();
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String email = auth.getName();
            UserVO user = userRepository.findByEmail(email);

            List<Review> reviewList = reviewDAO.getReviewById(reviewId);

            if (reviewList == null || reviewList.isEmpty()) {
                response.setMessage("Review not found!");
                return new ResponseEntity(response, HttpStatus.NOT_FOUND);
            }

            Review review = reviewList.get(0);

            // Check if user owns this review
            if (review.getUserVO().getId() != user.getId()) {
                response.setMessage("You can only delete your own reviews!");
                return new ResponseEntity(response, HttpStatus.FORBIDDEN);
            }

            reviewDAO.delete(review);

            response.setStatus(true);
            response.setMessage("Review deleted successfully!");
            return new ResponseEntity(response, HttpStatus.OK);

        } catch (Exception e) {
            e.printStackTrace();
            response.setMessage("Error: " + e.getMessage());
            return new ResponseEntity(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}