package com.logatron.catalog.persistence;

import com.logatron.catalog.domain.CatalogEnums.*;
import com.logatron.common.persistence.AuditedEntity;
import jakarta.persistence.*;
import java.util.UUID;

@Entity @Table(name="environment", uniqueConstraints=@UniqueConstraint(name="uq_environment_project_code",columnNames={"project_id","code"}))
public class EnvironmentEntity extends AuditedEntity {
    @Id private UUID id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="project_id") private ProjectEntity project;
    @Column(nullable=false,length=40) private String code;
    @Column(nullable=false,length=120) private String name;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=30) private EnvironmentType type;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=30) private Sensitivity sensitivity;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=30) private Status status;
    protected EnvironmentEntity(){}
    public EnvironmentEntity(UUID id,ProjectEntity project,String code,String name,EnvironmentType type,Sensitivity sensitivity,Status status){this.id=id;this.project=project;this.code=code;this.name=name;this.type=type;this.sensitivity=sensitivity;this.status=status;}
    public UUID getId(){return id;} public ProjectEntity getProject(){return project;} public String getCode(){return code;} public String getName(){return name;} public EnvironmentType getType(){return type;} public Sensitivity getSensitivity(){return sensitivity;} public Status getStatus(){return status;}
    public void update(String name,EnvironmentType type,Sensitivity sensitivity,Status status){this.name=name;this.type=type;this.sensitivity=sensitivity;this.status=status;}
}

