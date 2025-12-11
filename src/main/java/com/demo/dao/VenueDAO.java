package com.demo.dao;

import com.demo.model.VenueVO;
import java.util.List;

public interface VenueDAO {
    void save(VenueVO venueVO);
    List<VenueVO> getAll();
    List<VenueVO> getVenueById(int id);
    void delete(VenueVO venueVO);
}