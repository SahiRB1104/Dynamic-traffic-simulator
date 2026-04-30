package com.traffic.simulator.repository;

import com.traffic.simulator.entity.NodeEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NodeRepository extends JpaRepository<NodeEntity, Long> {
    Optional<NodeEntity> findByName(String name);
    boolean existsByName(String name);
}
