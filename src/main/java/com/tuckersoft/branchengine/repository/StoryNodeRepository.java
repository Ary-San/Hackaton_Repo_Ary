package com.tuckersoft.branchengine.repository;

import com.tuckersoft.branchengine.domain.StoryNode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StoryNodeRepository extends JpaRepository<StoryNode, Long> {

    Optional<StoryNode> findByNodeCode(String nodeCode);

    boolean existsByNodeCode(String nodeCode);

    List<StoryNode> findAllByOrderByIdAsc();
}
