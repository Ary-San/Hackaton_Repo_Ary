package com.tuckersoft.branchengine.repository;

import com.tuckersoft.branchengine.domain.RealityLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RealityLogRepository extends JpaRepository<RealityLog, Long> {

    List<RealityLog> findByDecisionIdOrderByCreatedAtAscIdAsc(Long decisionId);
}
