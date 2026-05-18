package com.suburbscore.suburb.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "suburbs",
       uniqueConstraints = @UniqueConstraint(columnNames = {"postcode", "name"}))
@SQLRestriction("is_deleted = false")
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "city_id")
    private City city;

    @Column(length = 100)
    private String lga;

    @Column(nullable = false, precision = 9, scale = 6)
    private BigDecimal latitude;

    @Column(nullable = false, precision = 9, scale = 6)
    private BigDecimal longitude;

    @Column(length = 50)
    private String region;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

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

    @OneToMany(mappedBy = "suburb", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<SuburbRentByType> rentData = new ArrayList<>();

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
