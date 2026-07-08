# syntax=docker/dockerfile:1
# Build from the repo root so the parent POM and sibling module POMs are in context:
#   docker build -f discovery-server/Dockerfile .

FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /workspace

# Copy all module POMs first so the dependency-resolution layer is cached until a POM changes.
# (The parent POM lists every module, so Maven needs all of them present to build the reactor.)
COPY pom.xml ./
COPY discovery-server/pom.xml discovery-server/
COPY api-gateway/pom.xml api-gateway/
COPY auth-service/pom.xml auth-service/
COPY product-service/pom.xml product-service/
COPY order-service/pom.xml order-service/
COPY inventory-service/pom.xml inventory-service/
COPY notification-service/pom.xml notification-service/

COPY discovery-server/src discovery-server/src
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -q -pl discovery-server -am -Dmaven.test.skip=true package

FROM eclipse-temurin:25-jre
WORKDIR /app
RUN useradd --system --uid 1001 appuser
COPY --from=build /workspace/discovery-server/target/discovery-server-1.0.0-SNAPSHOT.jar app.jar
USER appuser
EXPOSE 8761
ENV JAVA_OPTS=""
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
