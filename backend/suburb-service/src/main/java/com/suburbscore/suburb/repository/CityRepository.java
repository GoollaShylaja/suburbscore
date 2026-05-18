package com.suburbscore.suburb.repository;

import com.suburbscore.suburb.entity.City;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CityRepository extends JpaRepository<City, UUID> {

    Optional<City> findByNameAndState(String name, String state);
}
