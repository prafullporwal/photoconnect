# =============================================================================
# Single-stage Docker build for auth-service
# =============================================================================
# REQUIRES: auth-service/keys/{private,public}_key.pem must exist before build.
# Expects JAR pre-built on host (`mvn clean package -DskipTests`).
# =============================================================================
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S app && adduser -S -G app app
USER app
WORKDIR /app
COPY --chown=app:app auth-service/target/*.jar /app/app.jar
COPY --chown=app:app auth-service/keys/ /app/keys/
EXPOSE 8081
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
