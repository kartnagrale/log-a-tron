-- Fictional Phase 2 topology. IPs are IANA documentation ranges and hostnames use .local.test.
INSERT INTO server_node (id,environment_id,host_id,hostname,ip_address,status,labels,collector_status) VALUES
('30000000-0000-0000-0000-000000000006','20000000-0000-0000-0000-000000000005','payments-prod-01','payments-prod-01.local.test','198.51.100.21','ONLINE','{"zone":"local-a"}','UNKNOWN'),
('30000000-0000-0000-0000-000000000007','20000000-0000-0000-0000-000000000007','analytics-prod-01','analytics-prod-01.local.test','198.51.100.31','ONLINE','{"zone":"local-b"}','UNKNOWN'),
('30000000-0000-0000-0000-000000000008','20000000-0000-0000-0000-000000000008','support-dev-01','support-dev-01.local.test','203.0.113.41','ONLINE','{"zone":"local-dev"}','UNKNOWN');

INSERT INTO service_definition (id,project_id,service_key,display_name,owner_name,criticality,status) VALUES
('40000000-0000-0000-0000-000000000010','10000000-0000-0000-0000-000000000002','fulfillment-service','Fulfillment Service','Example Payments Team','CRITICAL','ACTIVE'),
('40000000-0000-0000-0000-000000000011','10000000-0000-0000-0000-000000000003','analytics-service','Analytics Service','Example Analytics Team','CRITICAL','ACTIVE'),
('40000000-0000-0000-0000-000000000012','10000000-0000-0000-0000-000000000004','support-service','Support Service','Example Support Team','HIGH','ACTIVE');

INSERT INTO service_instance (id,service_id,server_id,instance_key,version,status,first_seen_at,last_seen_at) VALUES
('50000000-0000-0000-0000-000000000011','40000000-0000-0000-0000-000000000010','30000000-0000-0000-0000-000000000006','fulfillment-prod-01','1.0.0','RUNNING',now(),now()),
('50000000-0000-0000-0000-000000000012','40000000-0000-0000-0000-000000000011','30000000-0000-0000-0000-000000000007','analytics-prod-01','1.0.0','RUNNING',now(),now()),
('50000000-0000-0000-0000-000000000013','40000000-0000-0000-0000-000000000012','30000000-0000-0000-0000-000000000008','case-dev-01','1.0.0','RUNNING',now(),now());

INSERT INTO log_source (id,service_instance_id,path_pattern,log_type,parser_profile,multiline_rule) VALUES
('60000000-0000-0000-0000-000000000005','50000000-0000-0000-0000-000000000005','/logs/notification/*.log','application','plaintext','^\\d{4}-\\d{2}-\\d{2}'),
('60000000-0000-0000-0000-000000000006','50000000-0000-0000-0000-000000000011','/logs/fulfillment/*.log','application','logback-json','^\\s+at '),
('60000000-0000-0000-0000-000000000007','50000000-0000-0000-0000-000000000012','/logs/analytics/*.log','application','logback-json','^\\s+at '),
('60000000-0000-0000-0000-000000000008','50000000-0000-0000-0000-000000000013','/logs/case/*.log','application','plaintext','^\\d{4}-\\d{2}-\\d{2}');