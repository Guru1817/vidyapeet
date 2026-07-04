package com.vidyapeet.institute.repository;

import com.vidyapeet.institute.Institute;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InstituteRepository extends JpaRepository<Institute, Long> {

    Optional<Institute> findBySlug(String slug);

    boolean existsBySlug(String slug);
}
