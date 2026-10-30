package com.logatron.catalog.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface CompanyRepository extends JpaRepository<CompanyEntity,UUID>{
 @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE) @org.springframework.data.jpa.repository.Query("select e from CompanyEntity e where e.id=:id") Optional<CompanyEntity> lockById(@org.springframework.data.repository.query.Param("id") UUID id);
Optional<CompanyEntity> findBySlug(String slug);List<CompanyEntity> findAllByOrderByName();}
