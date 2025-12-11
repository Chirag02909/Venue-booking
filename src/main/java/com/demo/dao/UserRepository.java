package com.demo.dao;

import com.demo.model.UserVO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<UserVO, Integer> {
    UserVO findByEmail(String email);
    boolean existsByEmail(String email);
}