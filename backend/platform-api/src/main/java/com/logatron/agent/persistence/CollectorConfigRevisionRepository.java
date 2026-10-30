package com.logatron.agent.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface CollectorConfigRevisionRepository extends JpaRepository<CollectorConfigRevisionEntity,UUID> {
    Optional<CollectorConfigRevisionEntity> findByAgent_IdAndConfigVersion(UUID agentId,long version);
    Optional<CollectorConfigRevisionEntity> findByAgent_IdAndConfigHash(UUID agentId,String hash);
    Optional<CollectorConfigRevisionEntity> findTopByAgent_IdOrderByConfigVersionDesc(UUID agentId);
    List<CollectorConfigRevisionEntity> findByAgent_IdOrderByConfigVersionDesc(UUID agentId);
}
