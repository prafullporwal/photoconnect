# =============================================================================
# Multi-stage Docker build for auth-service
# =============================================================================
# REQUIRES: auth-service/keys/{private,public}_key.pem must exist on disk
# before building. Locally: `make auth-keys` (or scripts/generate-keys.sh).
# In CI: the Jenkinsfile's Deploy stage generates them with openssl right
# before `docker compose up --build`.
#
# Keys are COPY'd into the image (NOT bind-mounted) because bind mounts from
# the Jenkins agent workspace can't be resolved by the host Docker daemon —
# the daemon would see an empty directory. For local single-developer CI this
# trade-off (secrets in image layers) is acceptable; in prod the keys come
# from AWS Secrets Manager and never touch the filesystem.
#
# Build:  docker build -t photoconnect/auth-service:dev -f auth-service/Dockerfile .
# Run:    docker run --rm -p 8081:8081 --network pc-net photoconnect/auth-service:dev
# =============================================================================

FROM maven:3.9-eclipse-temurin-21-alpine AS builder
WORKDIR /workspace
COPY pom.xml /workspace/pom.xml
COPY auth-service /workspace/auth-service
RUN mvn -B -f /workspace/auth-service/pom.xml -DskipTests package

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S app && adduser -S -G app app
USER app
WORKDIR /app
COPY --from=builder /workspace/auth-service/target/*.jar /app/app.jar
# Bake the JWT signing keypair in at build time. compose env sets
# AUTH_PRIVATE_KEY_PATH=/app/keys/private_key.pem and
# AUTH_PUBLIC_KEY_PATH=/app/keys/public_key.pem to point Spring here.
COPY --chown=app:app auth-service/keys/ /app/keys/
EXPOSE 8081
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
