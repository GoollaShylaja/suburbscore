package com.suburbscore.suburb.repository;

import com.suburbscore.suburb.entity.SchoolData;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SchoolDataRepository extends JpaRepository<SchoolData, UUID> {
    Optional<SchoolData> findBySuburbId(UUID suburbId);
}
