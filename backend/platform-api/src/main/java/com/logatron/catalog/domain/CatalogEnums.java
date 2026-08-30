package com.logatron.catalog.domain;

public final class CatalogEnums {
    private CatalogEnums() {}
    public enum Status { ACTIVE, INACTIVE, ONLINE, OFFLINE, RUNNING, STOPPED, UNKNOWN }
    public enum EnvironmentType { DEV, TEST, UAT, STAGING, PROD, OTHER }
    public enum Sensitivity { PUBLIC, INTERNAL, CONFIDENTIAL, RESTRICTED }
    public enum Classification { PUBLIC, INTERNAL, CONFIDENTIAL, RESTRICTED }
    public enum Criticality { LOW, MEDIUM, HIGH, CRITICAL }
    public enum CollectorStatus { ONLINE, OFFLINE, DEGRADED, UNKNOWN }
}

