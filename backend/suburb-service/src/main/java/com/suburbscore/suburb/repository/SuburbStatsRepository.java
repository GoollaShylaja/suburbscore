package com.suburbscore.suburb.repository;

import com.suburbscore.suburb.entity.SuburbStats;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SuburbStatsRepository extends JpaRepository<SuburbStats, UUID> {
    Optional<SuburbStats> findBySuburbId(UUID suburbId);
}
