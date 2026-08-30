package com.logatron.catalog.persistence;

import com.logatron.common.persistence.AuditedEntity;
import jakarta.persistence.*;
import java.util.UUID;

@Entity @Table(name="log_source",uniqueConstraints=@UniqueConstraint(name="uq_log_source_instance_path",columnNames={"service_instance_id","path_pattern"}))
public class LogSourceEntity extends AuditedEntity {
    @Id private UUID id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="service_instance_id") private ServiceInstanceEntity serviceInstance;
    @Column(name="path_pattern",nullable=false,length=1000) private String pathPattern;
    @Column(name="log_type",nullable=false,length=60) private String logType;
    @Column(name="parser_profile",nullable=false,length=100) private String parserProfile;
    @Column(name="multiline_rule",columnDefinition="text") private String multilineRule;
    @Column(nullable=false) private boolean enabled;
    @Column(name="config_version",nullable=false) private long configVersion;
    protected LogSourceEntity(){}
    public LogSourceEntity(UUID id,ServiceInstanceEntity serviceInstance,String pathPattern,String logType,String parserProfile,String multilineRule,boolean enabled){this.id=id;this.serviceInstance=serviceInstance;this.pathPattern=pathPattern;this.logType=logType;this.parserProfile=parserProfile;this.multilineRule=multilineRule;this.enabled=enabled;this.configVersion=1;}
    public UUID getId(){return id;} public ServiceInstanceEntity getServiceInstance(){return serviceInstance;} public String getPathPattern(){return pathPattern;} public String getLogType(){return logType;} public String getParserProfile(){return parserProfile;} public String getMultilineRule(){return multilineRule;} public boolean isEnabled(){return enabled;} public long getConfigVersion(){return configVersion;}
    public void update(String logType,String parserProfile,String multilineRule,boolean enabled){this.logType=logType;this.parserProfile=parserProfile;this.multilineRule=multilineRule;this.enabled=enabled;this.configVersion++;}
}
