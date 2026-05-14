# =============================================================================
# Multi-stage Docker build for discovery-service
# =============================================================================
# Stage 1 (builder) compiles the JAR using a full JDK image.
# Stage 2 (runtime) copies just the JAR into a slim JRE image — keeps the
# final image small and reduces attack surface.
#
# Build:  docker build -t photoconnect/discovery-service:dev .
# Run:    docker run --rm -p 8761:8761 photoconnect/discovery-service:dev
# =============================================================================

# ---- Stage 1: build -----------------------------------------------------------
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /workspace

# Copy parent POM and this module so Maven can resolve the reactor build.
# (We build only the discovery-service module here; in a CI pipeline you'd
# build the whole reactor once and copy artifacts.)
COPY pom.xml /workspace/pom.xml
COPY discovery-service /workspace/discovery-service

# Use the Maven Wrapper if present; otherwise install Maven.
RUN apk add --no-cache maven && \
    mvn -B -f /workspace/discovery-service/pom.xml -DskipTests package

# ---- Stage 2: runtime ---------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S app && adduser -S -G app app
USER app
WORKDIR /app

COPY --from=builder /workspace/discovery-service/target/*.jar /app/app.jar

EXPOSE 8761
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
