# =============================================================================
# Single-stage Docker build for customer-service
# =============================================================================
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S app && adduser -S -G app app
USER app
WORKDIR /app
COPY --chown=app:app customer-service/target/*.jar /app/app.jar
COPY --chown=app:app auth-service/keys/public_key.pem /app/keys/public_key.pem
EXPOSE 8083
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
