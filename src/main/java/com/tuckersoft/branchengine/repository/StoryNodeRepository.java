package com.tuckersoft.branchengine.repository;

import com.tuckersoft.branchengine.model.StoryNode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StoryNodeRepository extends JpaRepository<StoryNode, Long> {
    Optional<StoryNode> findByNodeCode(String nodeCode);
    boolean existsByNodeCode(String nodeCode);
}
