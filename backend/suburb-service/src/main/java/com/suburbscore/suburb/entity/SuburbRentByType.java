package com.suburbscore.suburb.entity;

import com.suburbscore.suburb.enums.PropertyType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "suburb_rent_by_type",
       uniqueConstraints = @UniqueConstraint(columnNames = {"suburb_id", "bedrooms", "property_type"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SuburbRentByType {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "suburb_id", nullable = false)
    private Suburb suburb;

    @Column(nullable = false)
    private Integer bedrooms;

    @Enumerated(EnumType.STRING)
    @Column(name = "property_type", nullable = false, length = 20)
    private PropertyType propertyType;

    @Column(name = "median_rent_weekly")
    private Integer medianRentWeekly;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = LocalDateTime.now();
    }
}
