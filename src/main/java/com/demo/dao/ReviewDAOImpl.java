package com.demo.dao;

import com.demo.model.Review;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.HibernateException;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ReviewDAOImpl implements ReviewDAO {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public void save(Review review) {
        try {
            if (review.getId() != null && review.getId() > 0) {
                entityManager.merge(review);
            } else {
                entityManager.persist(review);
            }
        } catch (HibernateException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<Review> getReviewsByVenueId(int venueId) {
        try {
            return entityManager.createQuery("FROM Review WHERE venueVO.id = :venueId ORDER BY createdAt DESC", Review.class)
                    .setParameter("venueId", venueId)
                    .getResultList();
        } catch (HibernateException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public List<Review> getReviewsByUserId(int userId) {
        try {
            return entityManager.createQuery("FROM Review WHERE userVO.id = :userId ORDER BY createdAt DESC", Review.class)
                    .setParameter("userId", userId)
                    .getResultList();
        } catch (HibernateException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public List<Review> getReviewById(Long id) {
        try {
            return entityManager.createQuery("FROM Review WHERE id = :id", Review.class)
                    .setParameter("id", id)
                    .getResultList();
        } catch (HibernateException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public Double getAverageRatingByVenueId(int venueId) {
        try {
            Double avg = entityManager.createQuery("SELECT AVG(r.rating) FROM Review r WHERE r.venueVO.id = :venueId", Double.class)
                    .setParameter("venueId", venueId)
                    .getSingleResult();
            return avg != null ? avg : 0.0;
        } catch (HibernateException e) {
            e.printStackTrace();
            return 0.0;
        }
    }

    @Override
    public void delete(Review review) {
        try {
            entityManager.remove(entityManager.contains(review) ? review : entityManager.merge(review));
        } catch (HibernateException e) {
            e.printStackTrace();
        }
    }
}