package com.tuckersoft.branchengine.repository;

import com.tuckersoft.branchengine.model.Playthrough;
import com.tuckersoft.branchengine.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlaythroughRepository extends JpaRepository<Playthrough, Long> {
    Optional<Playthrough> findByPlayerTag(String playerTag);
    boolean existsByPlayerTag(String playerTag);
    List<Playthrough> findByUserOrderByCreatedAtDesc(User user);
    List<Playthrough> findAllByOrderByCreatedAtDesc();
}
