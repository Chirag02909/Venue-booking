package com.demo.service;

import com.demo.dto.VenueDTO;
import org.springframework.http.ResponseEntity;

public interface VenueService {
    ResponseEntity getALlVenues();
    ResponseEntity createVenue(VenueDTO venueDTO);
    ResponseEntity getVenueById(int id);
    ResponseEntity deleteVenue(int id);
    ResponseEntity updateVenue(int id, VenueDTO venueDTO);
}