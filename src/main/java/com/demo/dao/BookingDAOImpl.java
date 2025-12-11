package com.demo.dao;

import com.demo.model.Booking;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.HibernateException;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Repository
public class BookingDAOImpl implements BookingDAO {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public void save(Booking booking) {
        try {
            if (booking.getId() != null && booking.getId() > 0) {
                entityManager.merge(booking);
            } else {
                entityManager.persist(booking);
            }
        } catch (HibernateException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<Booking> getAllBookings() {
        try {
            return entityManager.createQuery("FROM Booking ORDER BY id DESC", Booking.class).getResultList();
        } catch (HibernateException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public List<Booking> getBookingById(Long id) {
        try {
            return entityManager.createQuery("FROM Booking WHERE id = :id", Booking.class)
                    .setParameter("id", id)
                    .getResultList();
        } catch (HibernateException e) {
            e.printStackTrace();
            return null;
        }
    }

//    @Override
//    public List<Booking> getBookingsByUserId(int userId) {
//        try {
//            return entityManager.createQuery("FROM Booking WHERE userVO.id = :userId ORDER BY id DESC", Booking.class)
//                    .setParameter("userId", userId)
//                    .getResultList();
//        } catch (HibernateException e) {
//            e.printStackTrace();
//            return null;
//        }
//    }

    @Override
    public List<Booking> getBookingsByUserId(int userId) {
        try {
            List<Booking> bookings = entityManager.createQuery(
                            "FROM Booking WHERE userVO.id = :userId ORDER BY id DESC",
                            Booking.class)
                    .setParameter("userId", userId)
                    .getResultList();

            // Filter out bookings whose venueVO no longer exists
            return bookings.stream()
                    .filter(b -> {
                        try {
                            return b.getVenueVO() != null; // Only include valid ones
                        } catch (Exception e) {
                            return false;
                        }
                    })
                    .collect(Collectors.toList());

        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }


    @Override
    public List<Booking> getBookingByOwnerId(Long ownerId) {
        try {
            List<Booking> bookings = entityManager.createQuery(
                            "FROM Booking b WHERE b.venueVO.userVO.id = :ownerId ORDER BY b.id DESC",
                            Booking.class)
                    .setParameter("ownerId", ownerId)
                    .getResultList();

            // Filter out any bookings linked to deleted venues (just like you did above)
            return bookings.stream()
                    .filter(b -> {
                        try {
                            return b.getVenueVO() != null;
                        } catch (Exception e) {
                            return false;
                        }
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    @Override
    public List<Booking> getBookingsByVenueId(Long venueId) {
        try {
            return entityManager.createQuery("FROM Booking WHERE venueVO.id = :venueId ORDER BY startTime", Booking.class)
                    .setParameter("venueId", venueId)
                    .getResultList();
        } catch (HibernateException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void delete(Booking booking) {
        try {
            entityManager.remove(entityManager.contains(booking) ? booking : entityManager.merge(booking));
        } catch (HibernateException e) {
            e.printStackTrace();
        }
    }
}