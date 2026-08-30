package com.logatron.investigation.persistence;import org.springframework.data.jpa.repository.JpaRepository;import java.util.*;public interface InvestigationRepository extends JpaRepository<InvestigationEntity,UUID>{Optional<InvestigationEntity> findByIdAndOwnerUserId(UUID id,UUID ownerUserId);}

