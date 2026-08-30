package com.logatron.catalog.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface EnvironmentRepository extends JpaRepository<EnvironmentEntity,UUID>{List<EnvironmentEntity> findByProjectIdAndIdInOrderByCode(UUID projectId,Collection<UUID> ids);List<EnvironmentEntity> findByProjectIdOrderByCode(UUID projectId);Optional<EnvironmentEntity> findByProjectIdAndCode(UUID projectId,String code);}
