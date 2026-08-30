FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace
COPY mvnw mvnw.cmd pom.xml ./
COPY .mvn .mvn
COPY common-contracts/pom.xml common-contracts/pom.xml
COPY log-processor/pom.xml log-processor/pom.xml
COPY platform-api/pom.xml platform-api/pom.xml
RUN ./mvnw -q -pl platform-api -am dependency:go-offline
COPY common-contracts/src common-contracts/src
COPY log-processor/src log-processor/src
COPY platform-api/src platform-api/src
RUN ./mvnw -q -pl platform-api -am package -DskipTests

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S logatron && adduser -S logatron -G logatron
WORKDIR /app
COPY --from=build /workspace/platform-api/target/platform-api-0.1.0-SNAPSHOT.jar app.jar
USER logatron
EXPOSE 8080
ENTRYPOINT ["java","-XX:MaxRAMPercentage=75","-jar","/app/app.jar"]

