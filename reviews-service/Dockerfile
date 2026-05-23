# =============================================================================
# Single-stage Docker build for reviews-service
# =============================================================================
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S app && adduser -S -G app app
USER app
WORKDIR /app
COPY --chown=app:app reviews-service/target/*.jar /app/app.jar
COPY --chown=app:app auth-service/keys/public_key.pem /app/keys/public_key.pem
EXPOSE 8084
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
