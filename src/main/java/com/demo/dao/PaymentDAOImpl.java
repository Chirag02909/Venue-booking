package com.demo.dao;

import com.demo.model.Payment;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.HibernateException;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class PaymentDAOImpl implements PaymentDAO {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public void save(Payment payment) {
        try {
            if (payment.getId() != null && payment.getId() > 0) {
                entityManager.merge(payment);
            } else {
                entityManager.persist(payment);
            }
        } catch (HibernateException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<Payment> getPaymentsByBookingId(Long bookingId) {
        try {
            return entityManager.createQuery("FROM Payment WHERE booking.id = :bookingId", Payment.class)
                    .setParameter("bookingId", bookingId)
                    .getResultList();
        } catch (HibernateException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public List<Payment> getPaymentsByUserId(int userId) {
        try {
            return entityManager.createQuery("FROM Payment WHERE userVO.id = :userId ORDER BY id DESC", Payment.class)
                    .setParameter("userId", userId)
                    .getResultList();
        } catch (HibernateException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public List<Payment> getPaymentById(Long id) {
        try {
            return entityManager.createQuery("FROM Payment WHERE id = :id", Payment.class)
                    .setParameter("id", id)
                    .getResultList();
        } catch (HibernateException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public List<Payment> getAllPayments() {
        try {
            return entityManager.createQuery("FROM Payment ORDER BY id DESC", Payment.class).getResultList();
        } catch (HibernateException e) {
            e.printStackTrace();
            return null;
        }
    }
}