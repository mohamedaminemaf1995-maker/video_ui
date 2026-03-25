package com.local.ar44.repo;

import com.local.ar44.dto.AppConfig;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppConfigRepository extends JpaRepository<AppConfig, Long> {
}