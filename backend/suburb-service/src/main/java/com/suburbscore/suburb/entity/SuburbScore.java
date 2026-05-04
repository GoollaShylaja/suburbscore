package com.suburbscore.suburb.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "suburb_scores",
       uniqueConstraints = @UniqueConstraint(columnNames = {"suburb_id", "user_id"}))
@Getter
@Setter
@NoArgsConstructor
public class SuburbScore {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "suburb_id", nullable = false)
    private Suburb suburb;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "total_score", precision = 5, scale = 2)
    private BigDecimal totalScore;

    @Column(name = "commute_score", precision = 5, scale = 2)
    private BigDecimal commuteScore;

    @Column(name = "safety_score", precision = 5, scale = 2)
    private BigDecimal safetyScore;

    @Column(name = "schools_score", precision = 5, scale = 2)
    private BigDecimal schoolsScore;

    @Column(name = "walkability_score", precision = 5, scale = 2)
    private BigDecimal walkabilityScore;

    @Column(name = "value_score", precision = 5, scale = 2)
    private BigDecimal valueScore;

    @Column(name = "calculated_at", nullable = false, updatable = false)
    private LocalDateTime calculatedAt;

    @PrePersist
    void prePersist() {
        calculatedAt = LocalDateTime.now();
    }
}
