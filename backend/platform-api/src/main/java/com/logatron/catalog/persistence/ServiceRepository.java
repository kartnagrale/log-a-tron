package com.logatron.catalog.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface ServiceRepository extends JpaRepository<ServiceEntity,UUID>{List<ServiceEntity> findByProjectIdOrderByDisplayName(UUID projectId);List<ServiceEntity> findByProjectIdAndIdInOrderByDisplayName(UUID projectId,Collection<UUID> ids);Optional<ServiceEntity> findByProjectIdAndServiceKey(UUID projectId,String serviceKey);long countByProjectId(UUID projectId);}
