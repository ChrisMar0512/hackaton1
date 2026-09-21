package com.tuckersoft.branchengine.repository;

import com.tuckersoft.branchengine.model.Decision;
import com.tuckersoft.branchengine.model.Playthrough;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DecisionRepository extends JpaRepository<Decision, Long>, JpaSpecificationExecutor<Decision> {
    List<Decision> findByPlaythroughOrderByCreatedAtAsc(Playthrough playthrough);
    List<Decision> findByPlaythroughAndResolvedNodeCodeIsNotNullOrderByCreatedAtAsc(Playthrough playthrough);
}
