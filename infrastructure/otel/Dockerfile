FROM busybox:1.37.0-musl AS tools
RUN mkdir -p /otel-state
FROM otel/opentelemetry-collector-contrib:0.135.0
COPY --from=tools /bin/wget /bin/wget
COPY --from=tools --chown=10001:10001 /otel-state /var/lib/otelcol