# =============================================================================
# Multi-stage Docker build for config-service
# =============================================================================
# NOTE: when running this container, mount the config-repo into the container
# (or bake it in at build time). For Phase 2 the Git backend removes this
# concern — the container clones the config repo on startup.
#
# Build:  docker build -t photoconnect/config-service:dev -f config-service/Dockerfile .
# Run:    docker run --rm -p 8888:8888 \
#           -v "$(pwd)/config-repo:/app/config-repo:ro" \
#           photoconnect/config-service:dev
# =============================================================================

FROM maven:3.9-eclipse-temurin-21-alpine AS builder
WORKDIR /workspace
COPY pom.xml /workspace/pom.xml
COPY config-service /workspace/config-service
RUN mvn -B -f /workspace/config-service/pom.xml -DskipTests package

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S app && adduser -S -G app app
USER app
WORKDIR /app
COPY --from=builder /workspace/config-service/target/*.jar /app/app.jar
EXPOSE 8888
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
