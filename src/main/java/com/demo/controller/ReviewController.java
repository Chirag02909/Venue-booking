package com.demo.controller;

import com.demo.dto.ReviewDTO;
import com.demo.service.ReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reviews")
@CrossOrigin(origins = "*")
public class ReviewController {

    @Autowired
    private ReviewService reviewService;

    @PostMapping
    public ResponseEntity createReview(@RequestBody ReviewDTO reviewDTO) {
        return reviewService.createReview(reviewDTO);
    }

    @GetMapping("/venue/{venueId}")
    public ResponseEntity getReviewsByVenue(@PathVariable int venueId) {
        return reviewService.getReviewsByVenue(venueId);
    }

    @GetMapping("/my-reviews")
    public ResponseEntity getMyReviews() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        return reviewService.getMyReviews(email);
    }

    @DeleteMapping("/{reviewId}")
    public ResponseEntity deleteReview(@PathVariable Long reviewId) {
        return reviewService.deleteReview(reviewId);
    }
}