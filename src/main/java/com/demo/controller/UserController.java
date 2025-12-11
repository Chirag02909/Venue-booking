package com.demo.controller;

import com.demo.dto.BookingDTO;
import com.demo.service.BookingService;
import com.demo.service.VenueService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class UserController {

    @Autowired
    private VenueService venueService;

    @Autowired
    private BookingService bookingService;

    @GetMapping("/venues")
    public ResponseEntity getAllVenues() {
        return venueService.getALlVenues();
    }

    @GetMapping("/venues/{id}")
    public ResponseEntity getVenueById(@PathVariable int id) {
        return venueService.getVenueById(id);
    }

}