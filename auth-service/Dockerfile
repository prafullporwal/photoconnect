# =============================================================================
# Multi-stage Docker build for auth-service
# =============================================================================
# Build:  docker build -t photoconnect/auth-service:dev -f auth-service/Dockerfile .
# Run:    docker run --rm -p 8081:8081 \
#           -v "$(pwd)/auth-service/keys:/app/keys:ro" \
#           -e AUTH_PRIVATE_KEY_PATH=/app/keys/private_key.pem \
#           -e AUTH_PUBLIC_KEY_PATH=/app/keys/public_key.pem \
#           --network pc-net \
#           photoconnect/auth-service:dev
# =============================================================================

FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /workspace
COPY pom.xml /workspace/pom.xml
COPY auth-service /workspace/auth-service
RUN apk add --no-cache maven && \
    mvn -B -f /workspace/auth-service/pom.xml -DskipTests package

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S app && adduser -S -G app app
USER app
WORKDIR /app
COPY --from=builder /workspace/auth-service/target/*.jar /app/app.jar
EXPOSE 8081
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
