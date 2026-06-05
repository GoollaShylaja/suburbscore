package com.suburbscore.suburb.repository;

import com.suburbscore.suburb.entity.TransportData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface TransportDataRepository extends JpaRepository<TransportData, UUID> {
    Optional<TransportData> findBySuburbId(UUID suburbId);

    @Query("SELECT COUNT(t) FROM TransportData t WHERE t.suburb.isDeleted = false")
    long countForActiveSuburbs();
}
