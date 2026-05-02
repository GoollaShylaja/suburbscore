package com.suburbscore.user.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_preferences")
@Getter
@Setter
@NoArgsConstructor
public class UserPreferences {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    // ── INTENT
    @Enumerated(EnumType.STRING)
    @Column(name = "looking_to")
    private LookingTo lookingTo;

    // ── BUDGET
    @Column(name = "max_rent_per_week")
    private Integer maxRentPerWeek;

    @Column(name = "max_purchase_price")
    private Integer maxPurchasePrice;

    // ── PROPERTY
    @Column(name = "bedrooms_needed")
    private Integer bedroomsNeeded;

    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_property_type")
    private PropertyType preferredPropertyType;

    @Column(name = "needs_parking")
    private Boolean needsParking;

    @Column(name = "needs_garden")
    private Boolean needsGarden;

    @Column(name = "bathrooms_needed")
    private Integer bathroomsNeeded;

    // ── LIFESTYLE
    @Column(name = "workplace_suburb")
    private String workplaceSuburb;

    @Column(name = "partner_workplace_suburb")
    private String partnerWorkplaceSuburb;

    @Column(name = "has_children")
    private Boolean hasChildren;

    @Column(name = "has_pets")
    private Boolean hasPets;

    // ── IMPORTANCE WEIGHTS (1-5)
    @Column(name = "importance_commute")
    private Integer importanceCommute;

    @Column(name = "importance_safety")
    private Integer importanceSafety;

    @Column(name = "importance_schools")
    private Integer importanceSchools;

    @Column(name = "importance_walkability")
    private Integer importanceWalkability;

    @Column(name = "importance_parks")
    private Integer importanceParks;

    // ── NOTIFICATIONS
    @Column(name = "buy_mode_waitlist")
    private Boolean buyModeWaitlist;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
