package com.suburbscore.suburb.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "suburb_stats")
@Getter
@Setter
@NoArgsConstructor
public class SuburbStats {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "suburb_id", nullable = false, unique = true)
    private Suburb suburb;

    @Column(name = "median_rent_weekly")
    private Integer medianRentWeekly;

    @Column(name = "crime_index", precision = 5, scale = 2)
    private BigDecimal crimeIndex;

    @Column(name = "walkability_score", precision = 4, scale = 1)
    private BigDecimal walkabilityScore;

    @Column(name = "walkability_amenity_count")
    private Integer walkabilityAmenityCount;

    @Column(name = "parks_count")
    private Integer parksCount;

    @Column(name = "pct_houses")
    private Integer pctHouses;

    @Column(name = "pct_apartments")
    private Integer pctApartments;

    @Column(name = "pct_townhouses")
    private Integer pctTownhouses;

    @Column(name = "pct_units")
    private Integer pctUnits;

    @Column(name = "population")
    private Integer population;

    @Column(name = "median_age")
    private Integer medianAge;

    @Column(name = "unemployment_rate", precision = 4, scale = 2)
    private BigDecimal unemploymentRate;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "median_rent_updated_at")
    private LocalDateTime medianRentUpdatedAt;

    @Column(name = "crime_updated_at")
    private LocalDateTime crimeUpdatedAt;

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = LocalDateTime.now();
    }
}
