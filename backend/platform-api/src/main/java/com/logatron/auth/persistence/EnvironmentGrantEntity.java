package com.logatron.auth.persistence;

import com.logatron.catalog.persistence.EnvironmentEntity;
import com.logatron.catalog.persistence.ServiceEntity;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name="environment_grant")
public class EnvironmentGrantEntity {
    @Id private UUID id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="membership_id") private ProjectMembershipEntity membership;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="environment_id") private EnvironmentEntity environment;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="service_id") private ServiceEntity service;
    @Column(name="created_at",nullable=false) private Instant createdAt;
    protected EnvironmentGrantEntity(){}
    public EnvironmentGrantEntity(UUID id,ProjectMembershipEntity membership,EnvironmentEntity environment,ServiceEntity service){this.id=id;this.membership=membership;this.environment=environment;this.service=service;this.createdAt=Instant.now();}
    public UUID getId(){return id;} public ProjectMembershipEntity getMembership(){return membership;} public EnvironmentEntity getEnvironment(){return environment;} public ServiceEntity getService(){return service;}
}

