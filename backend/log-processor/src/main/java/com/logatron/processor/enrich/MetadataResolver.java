package com.logatron.processor.enrich;

import com.logatron.contracts.ingestion.ParserProfile;
import com.logatron.contracts.ingestion.RawLogEnvelope;
import com.logatron.processor.configuration.IngestionProperties;
import com.logatron.processor.parse.ProcessingException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class MetadataResolver {
    private final JdbcTemplate jdbc;
    private final IngestionProperties properties;
    private volatile Instant expires = Instant.EPOCH;
    private volatile Map<UUID, SourceMetadata> cache = Map.of();

    public MetadataResolver(JdbcTemplate jdbc, IngestionProperties properties) {
        this.jdbc = jdbc;
        this.properties = properties;
    }

    public SourceMetadata resolve(RawLogEnvelope envelope) {
        if (envelope.source() == null || envelope.source().logSourceId() == null) {
            throw new ProcessingException("UNKNOWN_SOURCE", "A registered logSourceId is required");
        }
        if (Instant.now().isAfter(expires)) refresh();
        SourceMetadata metadata = cache.get(envelope.source().logSourceId());
        if (metadata == null) throw new ProcessingException("UNKNOWN_SOURCE", "Log source is not registered or enabled");
        if (envelope.resource() == null
                || !Objects.equals(envelope.resource().projectId(), metadata.projectId())
                || !Objects.equals(envelope.resource().serviceInstanceId(), metadata.serviceInstanceId())) {
            throw new ProcessingException("SOURCE_SCOPE_MISMATCH", "Envelope resource identifiers do not match registered source metadata");
        }
        return metadata;
    }

    synchronized void refresh() {
        if (Instant.now().isBefore(expires)) return;
        String sql = "select c.id,c.name,p.id,p.name,p.classification,e.id,e.code,n.id,n.host_id,n.hostname,n.ip_address,"
                + "s.id,s.service_key,i.id,i.instance_key,l.id,l.path_pattern,l.log_type,l.parser_profile "
                + "from log_source l join service_instance i on i.id=l.service_instance_id "
                + "join service_definition s on s.id=i.service_id join server_node n on n.id=i.server_id "
                + "join environment e on e.id=n.environment_id join project p on p.id=e.project_id "
                + "join company c on c.id=p.company_id where l.enabled=true";
        Map<UUID, SourceMetadata> next = new ConcurrentHashMap<>();
        jdbc.query(sql, rs -> {
            ParserProfile profile = rs.getString(19).toLowerCase(Locale.ROOT).contains("json") ? ParserProfile.JSON : ParserProfile.PLAINTEXT;
            SourceMetadata metadata = new SourceMetadata(
                    rs.getObject(1, UUID.class), rs.getString(2), rs.getObject(3, UUID.class), rs.getString(4), rs.getString(5),
                    rs.getObject(6, UUID.class), rs.getString(7), rs.getObject(8, UUID.class), rs.getString(9), rs.getString(10),
                    rs.getString(11), rs.getObject(12, UUID.class), rs.getString(13), rs.getObject(14, UUID.class), rs.getString(15),
                    rs.getObject(16, UUID.class), rs.getString(17), rs.getString(18), profile);
            next.put(metadata.logSourceId(), metadata);
        });
        cache = Map.copyOf(next);
        expires = Instant.now().plus(properties.metadataCache().ttl());
    }

    public int cachedSources() { return cache.size(); }
}