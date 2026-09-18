package com.logatron.agent.persistence;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name="collector_config_revision", uniqueConstraints={
        @UniqueConstraint(name="uq_collector_config_revision_version",columnNames={"agent_id","config_version"}),
        @UniqueConstraint(name="uq_collector_config_revision_hash",columnNames={"agent_id","config_hash"})})
public class CollectorConfigRevisionEntity {
    @Id private UUID id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="agent_id",nullable=false) private CollectorAgentEntity agent;
    @Column(name="config_version",nullable=false) private long configVersion;
    @Column(name="config_hash",nullable=false,length=64) private String configHash;
    @Column(name="config_yaml",nullable=false,columnDefinition="text") private String configYaml;
    @Column(name="source_count",nullable=false) private int sourceCount;
    @Column(name="created_at",nullable=false,updatable=false) private Instant createdAt;

    protected CollectorConfigRevisionEntity() {}
    public CollectorConfigRevisionEntity(UUID id,CollectorAgentEntity agent,long configVersion,String configHash,String configYaml,int sourceCount,Instant createdAt){
        this.id=id;this.agent=agent;this.configVersion=configVersion;this.configHash=configHash;this.configYaml=configYaml;this.sourceCount=sourceCount;this.createdAt=createdAt;
    }
    public UUID getId(){return id;}
    public CollectorAgentEntity getAgent(){return agent;}
    public long getConfigVersion(){return configVersion;}
    public String getConfigHash(){return configHash;}
    public String getConfigYaml(){return configYaml;}
    public int getSourceCount(){return sourceCount;}
    public Instant getCreatedAt(){return createdAt;}
}
