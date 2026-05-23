# =============================================================================
# Single-stage Docker build for config-service
# =============================================================================
# Expects JAR pre-built on host (`mvn clean package -DskipTests`).
# config-repo/ is baked in so the container is self-contained.
# =============================================================================
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S app && adduser -S -G app app
USER app
WORKDIR /app
COPY --chown=app:app config-service/target/*.jar /app/app.jar
COPY --chown=app:app config-repo/ /app/config-repo/
EXPOSE 8888
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
