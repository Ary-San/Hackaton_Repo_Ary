package com.tuckersoft.branchengine.repository;

import com.tuckersoft.branchengine.domain.Decision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface DecisionRepository extends JpaRepository<Decision, Long>,
        JpaSpecificationExecutor<Decision> {

    /** El recorrido solo lista las decisiones que movieron la historia. */
    List<Decision> findByPlaythroughIdAndResolvedNodeCodeIsNotNullOrderByCreatedAtAscIdAsc(Long playthroughId);
}
