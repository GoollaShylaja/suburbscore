package com.suburbscore.suburb.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "school_data")
@Getter
@Setter
@NoArgsConstructor
public class SchoolData {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "suburb_id", nullable = false, unique = true)
    private Suburb suburb;

    @Column(name = "num_primary_schools")
    private Integer numPrimarySchools;

    @Column(name = "num_high_schools")
    private Integer numHighSchools;

    @Column(name = "avg_naplan_score", precision = 5, scale = 1)
    private BigDecimal avgNaplanScore;

    @Column(name = "best_school_name", length = 150)
    private String bestSchoolName;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = LocalDateTime.now();
    }
}
