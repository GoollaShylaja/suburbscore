package com.suburbscore.suburb.repository;

import com.suburbscore.suburb.entity.Suburb;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SuburbRepository extends JpaRepository<Suburb, UUID> {

    List<Suburb> findByPostcodeOrderByNameAsc(String postcode);

    List<Suburb> findByRegionIgnoreCase(String region);

    Optional<Suburb> findByNameIgnoreCase(String name);

    Page<Suburb> findAll(Pageable pageable);

    boolean existsByPostcode(String postcode);

    boolean existsByPostcodeAndNameIgnoreCase(String postcode, String name);

    @Query("""
            SELECT s FROM Suburb s
            WHERE LOWER(s.name) LIKE LOWER(CONCAT('%', :name, '%'))
            ORDER BY s.name ASC
            """)
    List<Suburb> searchByName(@Param("name") String name);
}
