package com.logatron.catalog.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface ProjectRepository extends JpaRepository<ProjectEntity,UUID>{
 @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE) @org.springframework.data.jpa.repository.Query("select e from ProjectEntity e where e.id=:id") Optional<ProjectEntity> lockById(@org.springframework.data.repository.query.Param("id") UUID id);
List<ProjectEntity> findAllByIdInOrderByName(Collection<UUID> ids);Optional<ProjectEntity> findByCompanyIdAndSlug(UUID companyId,String slug);}
