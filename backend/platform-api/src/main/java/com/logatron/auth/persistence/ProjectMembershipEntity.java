package com.logatron.auth.persistence;

import com.logatron.catalog.persistence.ProjectEntity;
import com.logatron.common.persistence.AuditedEntity;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name="project_membership",uniqueConstraints=@UniqueConstraint(name="uq_membership_user_project_role",columnNames={"user_id","project_id","role_id"}))
public class ProjectMembershipEntity extends AuditedEntity {
    @Id private UUID id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="user_id") private UserAccountEntity user;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="project_id") private ProjectEntity project;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="role_id") private RoleEntity role;
    @Column(name="valid_from",nullable=false) private Instant validFrom;
    @Column(name="valid_until") private Instant validUntil;
    protected ProjectMembershipEntity(){}
    public ProjectMembershipEntity(UUID id,UserAccountEntity user,ProjectEntity project,RoleEntity role,Instant validFrom,Instant validUntil){this.id=id;this.user=user;this.project=project;this.role=role;this.validFrom=validFrom;this.validUntil=validUntil;}
    public UUID getId(){return id;} public UserAccountEntity getUser(){return user;} public ProjectEntity getProject(){return project;} public RoleEntity getRole(){return role;} public Instant getValidFrom(){return validFrom;} public Instant getValidUntil(){return validUntil;}
    public boolean isActiveAt(Instant now){return !validFrom.isAfter(now)&&(validUntil==null||validUntil.isAfter(now));}
}

