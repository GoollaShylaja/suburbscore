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

    List<Suburb> findByPostcodeOrderBySuburbNameAsc(String postcode);

    List<Suburb> findByRegion_CodeIgnoreCase(String regionCode);

    Optional<Suburb> findBySuburbNameIgnoreCase(String suburbName);

    Optional<Suburb> findBySuburbNameIgnoreCaseAndPostcode(String suburbName, String postcode);

    Page<Suburb> findAll(Pageable pageable);

    boolean existsByPostcode(String postcode);

    boolean existsByPostcodeAndSuburbNameIgnoreCase(String postcode, String suburbName);

    @Query("""
            SELECT s FROM Suburb s
            WHERE LOWER(s.suburbName) LIKE LOWER(CONCAT('%', :name, '%'))
            ORDER BY s.suburbName ASC
            """)
    List<Suburb> searchByName(@Param("name") String name);
}
