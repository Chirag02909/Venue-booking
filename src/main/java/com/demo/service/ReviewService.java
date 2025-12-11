package com.demo.service;

import com.demo.dto.ReviewDTO;
import org.springframework.http.ResponseEntity;

public interface ReviewService {
    ResponseEntity createReview(ReviewDTO reviewDTO);
    ResponseEntity getReviewsByVenue(int venueId);
    ResponseEntity getMyReviews(String email);
    ResponseEntity deleteReview(Long reviewId);
}