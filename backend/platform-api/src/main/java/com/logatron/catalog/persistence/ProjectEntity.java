package com.logatron.catalog.persistence;

import com.logatron.catalog.domain.CatalogEnums.Classification;
import com.logatron.catalog.domain.CatalogEnums.Status;
import com.logatron.common.persistence.AuditedEntity;
import jakarta.persistence.*;
import java.util.UUID;

@Entity @Table(name="project", uniqueConstraints=@UniqueConstraint(name="uq_project_company_slug", columnNames={"company_id","slug"}))
public class ProjectEntity extends AuditedEntity {
    @Id private UUID id;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="company_id") private CompanyEntity company;
    @Column(nullable=false,length=80) private String slug;
    @Column(nullable=false,length=200) private String name;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=30) private Classification classification;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=30) private Status status;
    protected ProjectEntity() {}
    public ProjectEntity(UUID id, CompanyEntity company, String slug, String name, Classification classification, Status status){this.id=id;this.company=company;this.slug=slug;this.name=name;this.classification=classification;this.status=status;}
    public UUID getId(){return id;} public CompanyEntity getCompany(){return company;} public String getSlug(){return slug;} public String getName(){return name;} public Classification getClassification(){return classification;} public Status getStatus(){return status;}
    public void update(String name, Classification classification, Status status){this.name=name;this.classification=classification;this.status=status;}
}

