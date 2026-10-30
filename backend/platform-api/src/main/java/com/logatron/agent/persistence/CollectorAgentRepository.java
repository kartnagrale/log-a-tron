package com.logatron.agent.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface CollectorAgentRepository extends JpaRepository<CollectorAgentEntity,UUID> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select a from CollectorAgentEntity a where a.id=:id")
    Optional<CollectorAgentEntity> lockById(@org.springframework.data.repository.query.Param("id") UUID id);
    Optional<CollectorAgentEntity> findByServer_Id(UUID serverId);
    Optional<CollectorAgentEntity> findByAgentKey(String agentKey);
}
