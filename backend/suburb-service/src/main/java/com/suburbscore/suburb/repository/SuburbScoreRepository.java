package com.suburbscore.suburb.repository;

import com.suburbscore.suburb.entity.SuburbScore;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SuburbScoreRepository extends JpaRepository<SuburbScore, UUID> {
    Optional<SuburbScore> findBySuburbIdAndUserId(UUID suburbId, UUID userId);
    List<SuburbScore> findByUserIdOrderByTotalScoreDesc(UUID userId);
}
