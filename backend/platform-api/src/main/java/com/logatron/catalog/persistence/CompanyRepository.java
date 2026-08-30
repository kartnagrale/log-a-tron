package com.logatron.catalog.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface CompanyRepository extends JpaRepository<CompanyEntity,UUID>{Optional<CompanyEntity> findBySlug(String slug);}
