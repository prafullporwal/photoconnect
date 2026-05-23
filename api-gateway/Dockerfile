# =============================================================================
# Multi-stage Docker build for api-gateway
# =============================================================================
FROM maven:3.9-eclipse-temurin-21-alpine AS builder
WORKDIR /workspace
COPY pom.xml /workspace/pom.xml
COPY api-gateway /workspace/api-gateway
RUN mvn -B -f /workspace/api-gateway/pom.xml -DskipTests package

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S app && adduser -S -G app app
USER app
WORKDIR /app
COPY --from=builder /workspace/api-gateway/target/*.jar /app/app.jar
# Bake auth-service's PUBLIC key in at build time (gateway only verifies; it
# never needs the private key). Same rationale as auth-service/Dockerfile —
# bind mounts from the Jenkins agent workspace come up empty in our DinD
# setup, so we COPY instead. Build context must include auth-service/keys/.
COPY --chown=app:app auth-service/keys/public_key.pem /app/keys/public_key.pem
EXPOSE 8080
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
