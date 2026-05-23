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
EXPOSE 8080
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
