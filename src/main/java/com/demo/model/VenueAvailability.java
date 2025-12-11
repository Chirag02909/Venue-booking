package com.demo.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "venue_availability")
public class VenueAvailability {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "venue_id", nullable = false)
    private VenueVO venueVO;

    @Column(nullable = false)
    private LocalDate blockedDate;

    private String reason;

    @Enumerated(EnumType.STRING)
    private BlockType blockType = BlockType.MAINTENANCE;

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

    public LocalDate getBlockedDate() {
        return blockedDate;
    }

    public void setBlockedDate(LocalDate blockedDate) {
        this.blockedDate = blockedDate;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public BlockType getBlockType() {
        return blockType;
    }

    public void setBlockType(BlockType blockType) {
        this.blockType = blockType;
    }
}
