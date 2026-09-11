package com.logatron.agent.persistence;

import com.logatron.catalog.domain.CatalogEnums.CollectorStatus;
import com.logatron.catalog.persistence.ServerEntity;
import com.logatron.common.persistence.AuditedEntity;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name="collector_agent", uniqueConstraints={
        @UniqueConstraint(name="uq_collector_agent_server", columnNames="server_id"),
        @UniqueConstraint(name="uq_collector_agent_key", columnNames="agent_key")})
public class CollectorAgentEntity extends AuditedEntity {
    @Id private UUID id;
    @OneToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="server_id",nullable=false) private ServerEntity server;
    @Column(name="agent_key",nullable=false,length=160) private String agentKey;
    @Column(name="token_hash",nullable=false,length=64) private String tokenHash;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=30) private CollectorStatus status;
    @Column(name="collector_version",length=80) private String collectorVersion;
    @Column(name="applied_config_hash",length=64) private String appliedConfigHash;
    @Column(name="last_seen_at") private Instant lastSeenAt;
    @Column(name="last_error",columnDefinition="text") private String lastError;

    protected CollectorAgentEntity() {}

    public CollectorAgentEntity(UUID id,ServerEntity server,String agentKey,String tokenHash){
        this.id=id;this.server=server;this.agentKey=agentKey;this.tokenHash=tokenHash;this.status=CollectorStatus.UNKNOWN;
    }

    public UUID getId(){return id;}
    public ServerEntity getServer(){return server;}
    public String getAgentKey(){return agentKey;}
    public String getTokenHash(){return tokenHash;}
    public CollectorStatus getStatus(){return status;}
    public String getCollectorVersion(){return collectorVersion;}
    public String getAppliedConfigHash(){return appliedConfigHash;}
    public Instant getLastSeenAt(){return lastSeenAt;}
    public String getLastError(){return lastError;}

    public void rotateToken(String tokenHash){this.tokenHash=tokenHash;}

    public void heartbeat(CollectorStatus status,String collectorVersion,String appliedConfigHash,String lastError,Instant now){
        this.status=status==null?CollectorStatus.ONLINE:status;
        this.collectorVersion=trim(collectorVersion,80);
        this.appliedConfigHash=trim(appliedConfigHash,64);
        this.lastError=lastError==null?null:lastError.substring(0,Math.min(lastError.length(),4000));
        this.lastSeenAt=now;
    }

    private static String trim(String value,int max){
        if(value==null||value.isBlank())return null;
        String text=value.trim();return text.substring(0,Math.min(text.length(),max));
    }
}
