FROM busybox:1.37.0-musl AS tools

FROM grafana/tempo:3.0.2
COPY --from=tools /bin/wget /bin/wget
