package com.logatron.agent.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface CollectorAgentRepository extends JpaRepository<CollectorAgentEntity,UUID> {
    Optional<CollectorAgentEntity> findByServer_Id(UUID serverId);
    Optional<CollectorAgentEntity> findByAgentKey(String agentKey);
}
