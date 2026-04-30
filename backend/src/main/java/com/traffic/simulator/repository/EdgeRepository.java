package com.traffic.simulator.repository;

import com.traffic.simulator.entity.EdgeEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EdgeRepository extends JpaRepository<EdgeEntity, Long> {
    List<EdgeEntity> findByFromNode_Id(Long fromNodeId);
    List<EdgeEntity> findByToNode_Id(Long toNodeId);
}
