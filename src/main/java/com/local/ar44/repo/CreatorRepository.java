package com.local.ar44.repo;

import com.local.ar44.dto.Creator;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CreatorRepository extends JpaRepository<Creator, Long> {
    // Optionally add custom queries here
}
