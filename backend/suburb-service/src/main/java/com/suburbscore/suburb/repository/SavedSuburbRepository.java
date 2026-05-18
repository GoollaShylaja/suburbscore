package com.suburbscore.suburb.repository;

import com.suburbscore.suburb.entity.SavedSuburb;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SavedSuburbRepository extends JpaRepository<SavedSuburb, UUID> {
    List<SavedSuburb> findByUserId(UUID userId);
    Optional<SavedSuburb> findByUserIdAndSuburbId(UUID userId, UUID suburbId);
    boolean existsByUserIdAndSuburbId(UUID userId, UUID suburbId);
}
