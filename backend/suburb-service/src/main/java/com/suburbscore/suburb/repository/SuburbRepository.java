package com.suburbscore.suburb.repository;

import com.suburbscore.suburb.entity.Suburb;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SuburbRepository extends JpaRepository<Suburb, UUID> {

    List<Suburb> findByPostcodeOrderByNameAsc(String postcode);

    Page<Suburb> findAll(Pageable pageable);

    boolean existsByPostcode(String postcode);
}
