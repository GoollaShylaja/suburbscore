package com.suburbscore.suburb.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "transport_data")
@Getter
@Setter
@NoArgsConstructor
public class TransportData {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "suburb_id", nullable = false, unique = true)
    private Suburb suburb;

    @Column(name = "nearest_train_station", length = 100)
    private String nearestTrainStation;

    @Column(name = "train_station_walk_mins")
    private Integer trainStationWalkMins;

    @Column(name = "num_bus_routes")
    private Integer numBusRoutes;

    @Column(name = "cbd_commute_mins_train")
    private Integer cbdCommuteMinsTrain;

    @Column(name = "cbd_commute_mins_bus")
    private Integer cbdCommuteMinsBus;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = LocalDateTime.now();
    }
}
