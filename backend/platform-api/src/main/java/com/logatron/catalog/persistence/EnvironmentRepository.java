package com.logatron.catalog.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface EnvironmentRepository extends JpaRepository<EnvironmentEntity,UUID>{
 @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE) @org.springframework.data.jpa.repository.Query("select e from EnvironmentEntity e where e.id=:id") Optional<EnvironmentEntity> lockById(@org.springframework.data.repository.query.Param("id") UUID id);
List<EnvironmentEntity> findByProjectIdAndIdInOrderByCode(UUID projectId,Collection<UUID> ids);List<EnvironmentEntity> findByProjectIdOrderByCode(UUID projectId);Optional<EnvironmentEntity> findByProjectIdAndCode(UUID projectId,String code);}
