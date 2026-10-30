package com.logatron.catalog.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface ServiceRepository extends JpaRepository<ServiceEntity,UUID>{
 @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE) @org.springframework.data.jpa.repository.Query("select e from ServiceEntity e where e.id=:id") Optional<ServiceEntity> lockById(@org.springframework.data.repository.query.Param("id") UUID id);
List<ServiceEntity> findByProjectIdOrderByDisplayName(UUID projectId);List<ServiceEntity> findByProjectIdAndIdInOrderByDisplayName(UUID projectId,Collection<UUID> ids);Optional<ServiceEntity> findByProjectIdAndServiceKey(UUID projectId,String serviceKey);long countByProjectId(UUID projectId);}
