package com.local.ar44.repo;

import com.local.ar44.dto.AppAccessLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AppAccessLogRepository extends JpaRepository<AppAccessLog, Long> {
}
