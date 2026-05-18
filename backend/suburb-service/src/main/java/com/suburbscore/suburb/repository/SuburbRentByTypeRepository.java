package com.suburbscore.suburb.repository;

import com.suburbscore.suburb.entity.SuburbRentByType;
import com.suburbscore.suburb.enums.PropertyType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SuburbRentByTypeRepository extends JpaRepository<SuburbRentByType, UUID> {
    List<SuburbRentByType> findBySuburbId(UUID suburbId);
    Optional<SuburbRentByType> findBySuburbIdAndBedroomsAndPropertyType(
            UUID suburbId, Integer bedrooms, PropertyType propertyType);
}
