package com.suburbscore.suburb.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "suburbs",
       uniqueConstraints = @UniqueConstraint(columnNames = {"postcode", "name"}))
@Getter
@Setter
@NoArgsConstructor
public class Suburb {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 4)
    private String postcode;

    @Column(length = 100)
    private String lga;

    @Column(nullable = false, precision = 9, scale = 6)
    private BigDecimal latitude;

    @Column(nullable = false, precision = 9, scale = 6)
    private BigDecimal longitude;

    @Column(length = 50)
    private String region;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToOne(mappedBy = "suburb", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private SuburbStats stats;

    @OneToOne(mappedBy = "suburb", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private TransportData transportData;

    @OneToOne(mappedBy = "suburb", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private SchoolData schoolData;

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
