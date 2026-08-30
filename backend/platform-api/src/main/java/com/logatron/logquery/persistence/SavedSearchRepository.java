package com.logatron.logquery.persistence;
import org.springframework.data.jpa.repository.JpaRepository;import java.util.*;
public interface SavedSearchRepository extends JpaRepository<SavedSearchEntity,UUID>{List<SavedSearchEntity> findAllByOrderByUpdatedAtDesc();boolean existsByOwnerIdAndNameIgnoreCaseAndIdNot(UUID ownerId,String name,UUID id);}
