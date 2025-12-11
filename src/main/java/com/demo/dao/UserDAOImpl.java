package com.demo.dao;

import com.demo.model.UserVO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.HibernateException;
import org.springframework.stereotype.Repository;

@Repository
public class UserDAOImpl implements UserDAO{

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public void save(UserVO userVO) {
        try {
            entityManager.persist(userVO);
        } catch (HibernateException e) {
            e.printStackTrace();
        }
    }
}
