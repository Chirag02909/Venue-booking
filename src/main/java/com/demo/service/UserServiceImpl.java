package com.demo.service;

import com.demo.dao.UserDAO;
import com.demo.dao.UserRepository;
import com.demo.dto.Response;
import com.demo.dto.UserDTO;
import com.demo.model.Role;
import com.demo.model.UserVO;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class UserServiceImpl implements UserService{

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public ResponseEntity save(UserDTO userDTO) {
        Response response = new Response();

        // Field Validations
        if (userDTO.getFullname() == null || userDTO.getFullname().isEmpty()) {
            response.setMessage("Full name is required!");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
        if (userDTO.getEmail() == null || userDTO.getEmail().isEmpty()) {
            response.setMessage("Email is required!");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
        if (userDTO.getMobile() == null || userDTO.getMobile().isEmpty()) {
            response.setMessage("Mobile number is required!");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
        if (userDTO.getPassword() == null || userDTO.getPassword().isEmpty()) {
            response.setMessage("Password is required!");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        // Check if email already exists
        if (userRepository.existsByEmail(userDTO.getEmail())) {
            response.setMessage("Email already registered!");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        try {
            // Create new User entity
            UserVO user = new UserVO();
            user.setFullname(userDTO.getFullname());
            user.setEmail(userDTO.getEmail());
            user.setMobile(userDTO.getMobile());
            user.setPassword(passwordEncoder.encode(userDTO.getPassword()));

            // Role assignment logic
            if (userDTO.getRole() != null) {
                if (userDTO.getRole().toString().equalsIgnoreCase("ADMIN")) {
                    user.setRole(Role.ADMIN);
                } else if (userDTO.getRole().toString().equalsIgnoreCase("OWNER")) {
                    user.setRole(Role.OWNER);
                } else {
                    user.setRole(Role.USER);
                }
            } else {
                user.setRole(Role.USER); // default role
            }

            // Save user in DB
            userDAO.save(user);

            response.setStatus(true);
            response.setMessage("User registered successfully!");
            return new ResponseEntity(response, HttpStatus.OK);

        } catch (Exception e) {
            response.setStatus(false);
            response.setMessage("Error: " + e.getMessage());
            return new ResponseEntity(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}