package com.logatron.auth.persistence;
import com.logatron.auth.domain.PlatformRole;import org.springframework.data.jpa.repository.JpaRepository;import java.util.*;
public interface RoleRepository extends JpaRepository<RoleEntity,UUID>{Optional<RoleEntity> findByCode(PlatformRole code);}
