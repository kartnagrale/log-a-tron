package com.logatron.agent.configuration;

import com.logatron.common.error.ApiException;
import org.springframework.boot.context.properties.ConfigurationProperties;
import java.util.*;

@ConfigurationProperties("logatron.shipper")
public record ShipperManagementProperties(String validatorBinary,List<Destination> destinations) {
    public static final String COLLECTOR_VERSION="0.135.0";
    public ShipperManagementProperties {
        validatorBinary=validatorBinary==null?"":validatorBinary;
        destinations=destinations==null?List.of():List.copyOf(destinations);
    }
    public record Destination(String id,String name,String endpoint,boolean insecure,String caFile,String certFile,String keyFile,Set<UUID> projectIds,Set<UUID> environmentIds){
        public Destination { projectIds=projectIds==null?Set.of():Set.copyOf(projectIds);environmentIds=environmentIds==null?Set.of():Set.copyOf(environmentIds); }
        public boolean permits(UUID project,UUID environment){return (projectIds.isEmpty()||projectIds.contains(project))&&(environmentIds.isEmpty()||environmentIds.contains(environment));}
    }
    public Destination require(String endpoint,boolean insecure,UUID project,UUID environment){
        return destinations.stream().filter(d->d.endpoint().equals(endpoint)&&d.insecure()==insecure&&d.permits(project,environment)).findFirst()
                .orElseThrow(()->ApiException.invalid("Select a backend-managed destination authorized for this environment."));
    }
}
