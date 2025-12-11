package com.demo.service;

import com.demo.dto.UserDTO;
import org.springframework.http.ResponseEntity;

public interface UserService {
    ResponseEntity save(UserDTO userDTO);
}