package com.demo.model;

import jakarta.persistence.*;

@Entity
@Table(name = "venue_images")
public class VenueImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "venue_id", nullable = false)
    private VenueVO venueVO;

    @Column(nullable = false)
    private String imageUrl;

    private boolean isPrimary = false;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public VenueVO getVenueVO() {
        return venueVO;
    }

    public void setVenueVO(VenueVO venueVO) {
        this.venueVO = venueVO;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public boolean isPrimary() {
        return isPrimary;
    }

    public void setPrimary(boolean primary) {
        isPrimary = primary;
    }
}