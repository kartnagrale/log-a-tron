package com.logatron.catalog.persistence;

import com.logatron.catalog.domain.CatalogEnums.Status;
import com.logatron.common.persistence.AuditedEntity;
import jakarta.persistence.*;
import java.util.UUID;

@Entity @Table(name = "company")
public class CompanyEntity extends AuditedEntity {
    @Id private UUID id;
    @Column(nullable=false, unique=true, length=80) private String slug;
    @Column(nullable=false, length=200) private String name;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=30) private Status status;
    protected CompanyEntity() {}
    public CompanyEntity(UUID id, String slug, String name, Status status) { this.id=id; this.slug=slug; this.name=name; this.status=status; }
    public UUID getId(){return id;} public String getSlug(){return slug;} public String getName(){return name;} public Status getStatus(){return status;}
    public void update(String name, Status status){this.name=name;this.status=status;}
}

