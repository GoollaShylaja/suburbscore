package com.suburbscore.suburb.repository;

import com.suburbscore.suburb.entity.TransportData;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TransportDataRepository extends JpaRepository<TransportData, UUID> {
    Optional<TransportData> findBySuburbId(UUID suburbId);
}
