package com.logatron.agent.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface CollectorDeploymentRepository extends JpaRepository<CollectorDeploymentEntity,UUID>{
    Optional<CollectorDeploymentEntity> findByAgent_IdAndStatus(UUID agentId,String status);
    List<CollectorDeploymentEntity> findTop100ByAgent_IdOrderByRequestedAtDesc(UUID agentId);
}
