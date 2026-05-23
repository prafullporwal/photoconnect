# =============================================================================
# Single-stage Docker build for discovery-service
# =============================================================================
# Expects the JAR to be pre-built on the host:
#   mvn -B clean package -DskipTests
# Then `docker compose --profile apps up -d --build` packages it into the image.
#
# Switched away from in-image mvn because Docker Desktop's container network
# couldn't reach repo.maven.apache.org reliably on this host (timeouts even
# with --network=host). Host build uses the populated ~/.m2 cache and works.
# =============================================================================
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S app && adduser -S -G app app
USER app
WORKDIR /app
COPY --chown=app:app discovery-service/target/*.jar /app/app.jar
EXPOSE 8761
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
