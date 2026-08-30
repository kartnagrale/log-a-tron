package com.logatron.catalog.persistence;

import com.logatron.catalog.domain.CatalogEnums.*;
import com.logatron.common.persistence.AuditedEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.util.*;

@Entity @Table(name="service_definition",uniqueConstraints=@UniqueConstraint(name="uq_service_project_key",columnNames={"project_id","service_key"}))
public class ServiceEntity extends AuditedEntity {
    @Id private UUID id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="project_id") private ProjectEntity project;
    @Column(name="service_key",nullable=false,length=120) private String serviceKey;
    @Column(name="display_name",nullable=false,length=200) private String displayName;
    @Column(name="owner_name",length=200) private String owner;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=30) private Criticality criticality;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=30) private Status status;
    @JdbcTypeCode(SqlTypes.JSON) @Column(nullable=false,columnDefinition="jsonb") private Map<String,String> labels=new HashMap<>();
    protected ServiceEntity(){}
    public ServiceEntity(UUID id,ProjectEntity project,String serviceKey,String displayName,String owner,Criticality criticality,Status status,Map<String,String> labels){this.id=id;this.project=project;this.serviceKey=serviceKey;this.displayName=displayName;this.owner=owner;this.criticality=criticality;this.status=status;this.labels=labels==null?new HashMap<>():new HashMap<>(labels);}
    public UUID getId(){return id;} public ProjectEntity getProject(){return project;} public String getServiceKey(){return serviceKey;} public String getDisplayName(){return displayName;} public String getOwner(){return owner;} public Criticality getCriticality(){return criticality;} public Status getStatus(){return status;} public Map<String,String> getLabels(){return Map.copyOf(labels);}
    public void update(String displayName,String owner,Criticality criticality,Status status,Map<String,String> labels){this.displayName=displayName;this.owner=owner;this.criticality=criticality;this.status=status;this.labels=labels==null?new HashMap<>():new HashMap<>(labels);}
}

