package com.demo.service;

import com.demo.dao.UserRepository;
import com.demo.dao.VenueDAO;
import com.demo.dto.Response;
import com.demo.dto.VenueDTO;
import com.demo.model.UserVO;
import com.demo.model.VenueVO;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class VenueServiceImpl implements VenueService{

    @Autowired
    private VenueDAO venueDAO;

    @Autowired
    private UserRepository userRepository;

    @Override
    public ResponseEntity createVenue(VenueDTO venueDTO) {
        Response response = new Response();

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        UserVO user = userRepository.findByEmail(email);

        if (user == null) {
            response.setMessage("User not found!");
            return new ResponseEntity(response, HttpStatus.UNAUTHORIZED);
        }

        if(venueDTO.getName() == null || venueDTO.getName().isEmpty())
        {
            response.setMessage("Name is required !");
            return new ResponseEntity(response,HttpStatus.BAD_REQUEST);
        }

        if(venueDTO.getLocation() == null || venueDTO.getLocation().isEmpty())
        {
            response.setMessage("Location is required !");
            return new ResponseEntity(response,HttpStatus.BAD_REQUEST);
        }

        if(venueDTO.getDescription() == null || venueDTO.getDescription().isEmpty())
        {
            response.setMessage("Description is required !");
            return new ResponseEntity(response,HttpStatus.BAD_REQUEST);
        }

        if(venueDTO.getPricePerDay() <= 0)
        {
            response.setMessage("Price per day must be greater than 0!");
            return new ResponseEntity(response,HttpStatus.BAD_REQUEST);
        }

        if(venueDTO.getCapacity() <= 0)
        {
            response.setMessage("Capacity must be greater than 0!");
            return new ResponseEntity(response,HttpStatus.BAD_REQUEST);
        }

        try {

            VenueVO v = new VenueVO();
            v.setUserVO(user);
            v.setName(venueDTO.getName());
            v.setCapacity(venueDTO.getCapacity());
            v.setLocation(venueDTO.getLocation());
            v.setPricePerDay(venueDTO.getPricePerDay());
            v.setDescription(venueDTO.getDescription());

            if (venueDTO.getImageUrl() != null && !venueDTO.getImageUrl().isEmpty()) {
                v.setImageUrl(venueDTO.getImageUrl());
            }

            venueDAO.save(v);

            response.setMessage("Venue created successfully !");
            response.setStatus(true);
            response.setData(v);
            return new ResponseEntity(response,HttpStatus.OK);

        }catch (Exception e) {
            response.setMessage(e.getMessage());
            return new ResponseEntity(response,HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ResponseEntity getALlVenues() {
        Response response = new Response();
        try
        {
            List<VenueDTO> venueDTOList = new ArrayList<VenueDTO>();
            List<VenueVO> venueVOList = venueDAO.getAll();

            if(venueVOList != null && !venueVOList.isEmpty()) {
                venueVOList.forEach(venue -> {
                    VenueDTO dto = new VenueDTO();

                    dto.setId(venue.getId());
                    dto.setName(venue.getName());
                    dto.setLocation(venue.getLocation());
                    dto.setPricePerDay(venue.getPricePerDay());
                    dto.setCapacity(venue.getCapacity());
                    dto.setDescription(venue.getDescription());
                    dto.setImageUrl(venue.getImageUrl());

                    venueDTOList.add(dto);
                });
            }

            response.setData(venueDTOList);
            response.setStatus(true);
            response.setMessage(venueDTOList.isEmpty() ? "No venues found" : "");
            return new ResponseEntity(response, HttpStatus.OK);
        }
        catch (Exception e) {
            e.printStackTrace();
            response.setMessage(e.getMessage());
            return new ResponseEntity(response,HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ResponseEntity getVenueById(int id) {
        Response response = new Response();
        try {
            List<VenueVO> venueVOList = venueDAO.getVenueById(id);

            if(venueVOList == null || venueVOList.isEmpty()) {
                response.setMessage(String.format("Venue with id = %d not found", id));
                return new ResponseEntity(response, HttpStatus.NOT_FOUND);
            }

            VenueVO venueVO = venueVOList.get(0);
            VenueDTO venueDTO = new VenueDTO();

            venueDTO.setId(venueVO.getId());
            venueDTO.setName(venueVO.getName());
            venueDTO.setCapacity(venueVO.getCapacity());
            venueDTO.setLocation(venueVO.getLocation());
            venueDTO.setPricePerDay(venueVO.getPricePerDay());
            venueDTO.setDescription(venueVO.getDescription());
            venueDTO.setImageUrl(venueVO.getImageUrl());

            response.setData(venueDTO);
            response.setStatus(true);

            return new ResponseEntity(response,HttpStatus.OK);
        }
        catch (Exception e) {
            response.setMessage(e.getMessage());
            return new ResponseEntity(response,HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ResponseEntity deleteVenue(int id) {
        Response response = new Response();
        try {
            List<VenueVO> venueVOList = venueDAO.getVenueById(id);

            if(venueVOList == null || venueVOList.isEmpty())
            {
                response.setMessage(String.format("Venue with id = %d not found", id));
                return new ResponseEntity(response,HttpStatus.BAD_REQUEST);
            }
            VenueVO venueVO = venueVOList.get(0);
            venueDAO.delete(venueVO);
            response.setMessage("Venue deleted successfully!");
            response.setStatus(true);
            return new ResponseEntity(response,HttpStatus.OK);
        }
        catch (Exception e) {
            response.setMessage(e.getMessage());
            return new ResponseEntity(response,HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ResponseEntity updateVenue(int id, VenueDTO venueDTO) {
        Response response = new Response();
        try {
            List<VenueVO> venueVOList = venueDAO.getVenueById(id);

            if(venueVOList == null || venueVOList.isEmpty()) {
                response.setMessage(String.format("Venue with id = %d not found", id));
                return new ResponseEntity(response, HttpStatus.NOT_FOUND);
            }

            if(venueDTO.getName() == null || venueDTO.getName().isEmpty())
            {
                response.setMessage("Name is required !");
                return new ResponseEntity(response,HttpStatus.BAD_REQUEST);
            }

            if(venueDTO.getLocation() == null || venueDTO.getLocation().isEmpty())
            {
                response.setMessage("Location is required !");
                return new ResponseEntity(response,HttpStatus.BAD_REQUEST);
            }

            if(venueDTO.getDescription() == null || venueDTO.getDescription().isEmpty())
            {
                response.setMessage("Description is required !");
                return new ResponseEntity(response,HttpStatus.BAD_REQUEST);
            }

            VenueVO venueVO = venueVOList.get(0);
            venueVO.setName(venueDTO.getName());
            venueVO.setCapacity(venueDTO.getCapacity());
            venueVO.setLocation(venueDTO.getLocation());
            venueVO.setPricePerDay(venueDTO.getPricePerDay());
            venueVO.setDescription(venueDTO.getDescription());
            venueVO.setImageUrl(venueDTO.getImageUrl());

            venueDAO.save(venueVO);

            response.setMessage("Venue updated successfully!");
            response.setStatus(true);
            response.setData(venueVO);
            return new ResponseEntity(response,HttpStatus.OK);
        }
        catch (Exception e) {
            response.setMessage(e.getMessage());
            return new ResponseEntity(response,HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}