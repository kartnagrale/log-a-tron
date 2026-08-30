package com.logatron.auth.persistence;
import org.springframework.data.jpa.repository.*;import org.springframework.data.repository.query.Param;import java.util.*;
public interface EnvironmentGrantRepository extends JpaRepository<EnvironmentGrantEntity,UUID>{@Query("select g from EnvironmentGrantEntity g join fetch g.environment left join fetch g.service where g.membership.id in :membershipIds") List<EnvironmentGrantEntity> findDetailedByMembershipIds(@Param("membershipIds")Collection<UUID> membershipIds);}
