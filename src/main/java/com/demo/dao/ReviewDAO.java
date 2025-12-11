package com.demo.dao;

import com.demo.model.Review;
import java.util.List;

public interface ReviewDAO {
    void save(Review review);
    List<Review> getReviewsByVenueId(int venueId);
    List<Review> getReviewsByUserId(int userId);
    List<Review> getReviewById(Long id);
    Double getAverageRatingByVenueId(int venueId);
    void delete(Review review);
}