package com.logatron.auth.persistence;

import com.logatron.auth.domain.PlatformRole;
import jakarta.persistence.*;
import java.util.UUID;

@Entity @Table(name="role_definition")
public class RoleEntity {
    @Id private UUID id;
    @Enumerated(EnumType.STRING) @Column(nullable=false,unique=true,length=40) private PlatformRole code;
    @Column(nullable=false,length=300) private String description;
    protected RoleEntity(){}
    public UUID getId(){return id;} public PlatformRole getCode(){return code;} public String getDescription(){return description;}
}

