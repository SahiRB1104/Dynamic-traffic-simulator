package com.traffic.simulator.repository;

import com.traffic.simulator.entity.NodeEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

public interface NodeRepository extends JpaRepository<NodeEntity, Long> {
    Optional<NodeEntity> findByName(String name);
    boolean existsByName(String name);

    @Query(value = "SELECT * FROM nodes ORDER BY (lat - :lat)*(lat - :lat) + (lon - :lon)*(lon - :lon) ASC LIMIT 1", nativeQuery = true)
    Optional<NodeEntity> findNearestNode(@Param("lat") double lat, @Param("lon") double lon);
}
