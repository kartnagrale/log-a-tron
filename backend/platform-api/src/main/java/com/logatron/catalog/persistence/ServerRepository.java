package com.logatron.catalog.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface ServerRepository extends JpaRepository<ServerEntity,UUID>{List<ServerEntity> findByEnvironmentIdOrderByHostname(UUID environmentId);Optional<ServerEntity> findByEnvironmentIdAndHostId(UUID environmentId,String hostId);long countByEnvironmentProjectId(UUID projectId);}
