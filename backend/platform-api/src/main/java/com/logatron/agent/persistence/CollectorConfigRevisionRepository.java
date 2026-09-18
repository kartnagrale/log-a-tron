package com.logatron.agent.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface CollectorConfigRevisionRepository extends JpaRepository<CollectorConfigRevisionEntity,UUID> {
    Optional<CollectorConfigRevisionEntity> findTopByAgent_IdOrderByConfigVersionDesc(UUID agentId);
    List<CollectorConfigRevisionEntity> findByAgent_IdOrderByConfigVersionDesc(UUID agentId);
}
