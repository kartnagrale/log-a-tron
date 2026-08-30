package com.logatron.auth.persistence;

import com.logatron.catalog.domain.CatalogEnums.Status;
import com.logatron.common.persistence.AuditedEntity;
import jakarta.persistence.*;
import java.util.UUID;

@Entity @Table(name="user_account",uniqueConstraints={@UniqueConstraint(name="uq_user_issuer_subject",columnNames={"issuer","subject"}),@UniqueConstraint(name="uq_user_issuer_username",columnNames={"issuer","username"})})
public class UserAccountEntity extends AuditedEntity {
    @Id private UUID id;
    @Column(nullable=false,length=255) private String issuer;
    @Column(nullable=false,length=255) private String subject;
    @Column(nullable=false,length=120) private String username;
    @Column(name="display_name",nullable=false,length=200) private String displayName;
    @Column(length=320) private String email;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=30) private Status status;
    protected UserAccountEntity(){}
    public UserAccountEntity(UUID id,String issuer,String subject,String username,String displayName,String email){this.id=id;this.issuer=issuer;this.subject=subject;this.username=username;this.displayName=displayName;this.email=email;this.status=Status.ACTIVE;}
    public UUID getId(){return id;} public String getIssuer(){return issuer;} public String getSubject(){return subject;} public String getUsername(){return username;} public String getDisplayName(){return displayName;} public String getEmail(){return email;} public Status getStatus(){return status;}
}

