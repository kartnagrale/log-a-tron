package com.logatron.catalog.persistence;

import com.logatron.catalog.domain.CatalogEnums.Status;
import com.logatron.common.persistence.AuditedEntity;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name="service_instance",uniqueConstraints=@UniqueConstraint(name="uq_service_instance_server_key",columnNames={"server_id","instance_key"}))
public class ServiceInstanceEntity extends AuditedEntity {
    @Id private UUID id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="service_id") private ServiceEntity service;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="server_id") private ServerEntity server;
    @Column(name="instance_key",nullable=false,length=180) private String instanceKey;
    @Column(length=80) private String version;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=30) private Status status;
    @Column(name="first_seen_at") private Instant firstSeenAt;
    @Column(name="last_seen_at") private Instant lastSeenAt;
    protected ServiceInstanceEntity(){}
    public ServiceInstanceEntity(UUID id,ServiceEntity service,ServerEntity server,String instanceKey,String version,Status status){this.id=id;this.service=service;this.server=server;this.instanceKey=instanceKey;this.version=version;this.status=status;}
    public UUID getId(){return id;} public ServiceEntity getService(){return service;} public ServerEntity getServer(){return server;} public String getInstanceKey(){return instanceKey;} public String getVersion(){return version;} public Status getStatus(){return status;} public Instant getFirstSeenAt(){return firstSeenAt;} public Instant getLastSeenAt(){return lastSeenAt;}
    public void update(String version,Status status){this.version=version;this.status=status;}
}

