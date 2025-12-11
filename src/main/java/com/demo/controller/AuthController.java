package com.demo.controller;

import com.demo.config.JwtUtil;
import com.demo.dao.UserRepository;
import com.demo.dto.Response;
import com.demo.dto.UserDTO;
import com.demo.model.UserVO;
import com.demo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/register")
    public ResponseEntity register(@RequestBody UserDTO userDTO) {
        return userService.save(userDTO);
    }

    @PostMapping("/login")
    public ResponseEntity login(@RequestBody UserDTO userDTO) {
        Response response = new Response();

        if (userDTO.getEmail() == null || userDTO.getEmail().isEmpty()) {
            response.setMessage("Email is required!");
            return new ResponseEntity(response, HttpStatus.BAD_REQUEST);
        }

        if (userDTO.getPassword() == null || userDTO.getPassword().isEmpty()) {
            response.setMessage("Password is required!");
            return new ResponseEntity(response, HttpStatus.BAD_REQUEST);
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(userDTO.getEmail(), userDTO.getPassword())
            );

            if (authentication.isAuthenticated()) {
                UserVO user = userRepository.findByEmail(userDTO.getEmail());

                String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());

                Map<String, Object> data = new HashMap<>();
                data.put("token", token);
                data.put("email", user.getEmail());
                data.put("fullname", user.getFullname());
                data.put("role", user.getRole().name());
                data.put("userId", user.getId());

                response.setStatus(true);
                response.setMessage("Login successful!");
                response.setData(data);
                return new ResponseEntity(response, HttpStatus.OK);
            }
        } catch (AuthenticationException e) {
            response.setMessage("Invalid email or password!");
            return new ResponseEntity(response, HttpStatus.UNAUTHORIZED);
        }

        response.setMessage("Authentication failed!");
        return new ResponseEntity(response, HttpStatus.UNAUTHORIZED);
    }
}