package com.demo.dao;

import com.demo.model.VenueVO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.HibernateException;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class VenueDAOImpl implements VenueDAO {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public void save(VenueVO venueVO) {
        try {
            if (venueVO.getId() > 0) {
                entityManager.merge(venueVO);
            } else {
                entityManager.persist(venueVO);
            }
        } catch (HibernateException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<VenueVO> getAll() {
        try {
            return entityManager.createQuery("FROM VenueVO", VenueVO.class).getResultList();
        } catch (HibernateException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public List<VenueVO> getVenueById(int id) {
        try {
            return entityManager.createQuery("FROM VenueVO WHERE id = :id", VenueVO.class)
                    .setParameter("id", id)
                    .getResultList();
        } catch (HibernateException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void delete(VenueVO venueVO) {
        try {
            entityManager.remove(entityManager.contains(venueVO) ? venueVO : entityManager.merge(venueVO));
        } catch (HibernateException e) {
            e.printStackTrace();
        }
    }
}