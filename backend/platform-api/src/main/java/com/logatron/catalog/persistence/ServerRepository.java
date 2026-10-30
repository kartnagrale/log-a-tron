package com.logatron.catalog.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface ServerRepository extends JpaRepository<ServerEntity,UUID>{
 @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE) @org.springframework.data.jpa.repository.Query("select e from ServerEntity e where e.id=:id") Optional<ServerEntity> lockById(@org.springframework.data.repository.query.Param("id") UUID id);
List<ServerEntity> findByEnvironmentIdOrderByHostname(UUID environmentId);Optional<ServerEntity> findByEnvironmentIdAndHostId(UUID environmentId,String hostId);long countByEnvironmentProjectId(UUID projectId);}
