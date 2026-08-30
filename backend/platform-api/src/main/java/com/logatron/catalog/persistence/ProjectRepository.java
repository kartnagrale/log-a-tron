package com.logatron.catalog.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface ProjectRepository extends JpaRepository<ProjectEntity,UUID>{List<ProjectEntity> findAllByIdInOrderByName(Collection<UUID> ids);Optional<ProjectEntity> findByCompanyIdAndSlug(UUID companyId,String slug);}
