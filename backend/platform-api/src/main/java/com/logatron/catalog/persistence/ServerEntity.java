package com.logatron.catalog.persistence;

import com.logatron.catalog.domain.CatalogEnums.*;
import com.logatron.common.persistence.AuditedEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.*;

@Entity @Table(name="server_node",uniqueConstraints=@UniqueConstraint(name="uq_server_environment_host",columnNames={"environment_id","host_id"}))
public class ServerEntity extends AuditedEntity {
    @Id private UUID id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="environment_id") private EnvironmentEntity environment;
    @Column(name="host_id",nullable=false,length=160) private String hostId;
    @Column(nullable=false,length=255) private String hostname;
    @Column(name="ip_address",length=45) private String ipAddress;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=30) private Status status;
    @JdbcTypeCode(SqlTypes.JSON) @Column(nullable=false,columnDefinition="jsonb") private Map<String,String> labels = new HashMap<>();
    @Enumerated(EnumType.STRING) @Column(name="collector_status",nullable=false,length=30) private CollectorStatus collectorStatus;
    @Column(name="collector_last_seen_at") private Instant collectorLastSeenAt;
    protected ServerEntity(){}
    public ServerEntity(UUID id,EnvironmentEntity environment,String hostId,String hostname,String ipAddress,Status status,Map<String,String> labels){this.id=id;this.environment=environment;this.hostId=hostId;this.hostname=hostname;this.ipAddress=ipAddress;this.status=status;this.labels=labels==null?new HashMap<>():new HashMap<>(labels);this.collectorStatus=CollectorStatus.UNKNOWN;}
    public UUID getId(){return id;} public EnvironmentEntity getEnvironment(){return environment;} public String getHostId(){return hostId;} public String getHostname(){return hostname;} public String getIpAddress(){return ipAddress;} public Status getStatus(){return status;} public Map<String,String> getLabels(){return Map.copyOf(labels);} public CollectorStatus getCollectorStatus(){return collectorStatus;} public Instant getCollectorLastSeenAt(){return collectorLastSeenAt;}
    public void update(String hostname,String ipAddress,Status status,Map<String,String> labels){this.hostname=hostname;this.ipAddress=ipAddress;this.status=status;this.labels=labels==null?new HashMap<>():new HashMap<>(labels);}
}

