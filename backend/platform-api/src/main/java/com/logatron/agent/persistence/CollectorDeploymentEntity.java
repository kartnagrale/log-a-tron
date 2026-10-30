package com.logatron.agent.persistence;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name="collector_deployment")
public class CollectorDeploymentEntity {
    @Id private UUID id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="agent_id") private CollectorAgentEntity agent;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="revision_id") private CollectorConfigRevisionEntity revision;
    @Column(name="previous_version") private Long previousVersion;
    @Column(nullable=false,length=30) private String status;
    @Column(nullable=false,length=30) private String action;
    @Column(name="requested_by",nullable=false) private UUID requestedBy;
    @Column(name="requested_at",nullable=false) private Instant requestedAt;
    @Column(name="completed_at") private Instant completedAt;
    @Column(name="last_error",columnDefinition="text") private String lastError;
    @Version @Column(name="row_version") private long rowVersion;
    protected CollectorDeploymentEntity(){}
    public CollectorDeploymentEntity(CollectorAgentEntity agent,CollectorConfigRevisionEntity revision,Long previous,String action,UUID actor){this.id=UUID.randomUUID();this.agent=agent;this.revision=revision;this.previousVersion=previous;this.action=action;this.requestedBy=actor;this.requestedAt=Instant.now();this.status="PENDING";}
    public UUID getId(){return id;} public CollectorConfigRevisionEntity getRevision(){return revision;} public Long getPreviousVersion(){return previousVersion;} public String getStatus(){return status;} public String getAction(){return action;} public Instant getRequestedAt(){return requestedAt;} public Instant getCompletedAt(){return completedAt;} public String getLastError(){return lastError;}
    public void complete(String status,String error){this.status=status;this.lastError=error;this.completedAt=Instant.now();}
}
