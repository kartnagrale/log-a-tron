package com.logatron.catalog.persistence;
import org.springframework.data.jpa.repository.*;import org.springframework.data.repository.query.Param;import java.util.*;
public interface LogSourceRepository extends JpaRepository<LogSourceEntity,UUID>{@Query("select l from LogSourceEntity l join fetch l.serviceInstance i join fetch i.service join fetch i.server where i.id=:instanceId order by l.pathPattern") List<LogSourceEntity> findDetailedByServiceInstanceId(@Param("instanceId")UUID instanceId);}
