# =============================================================================
# Multi-stage Docker build for config-service
# =============================================================================
# config-repo/ is COPY'd into the image at build time, so the container is
# self-contained — no host bind-mount needed. Rebuilding the image picks up
# any config-repo edits. (Previously this was bind-mounted, but bind mounts
# from the Jenkins agent workspace can't be resolved by the host Docker daemon
# in our docker-in-docker setup — the daemon sees an empty directory.)
#
# Phase 2 will switch to the Git backend (application-git.properties) and
# this baked-in copy goes away.
#
# Build:  docker build -t photoconnect/config-service:dev -f config-service/Dockerfile .
# Run:    docker run --rm -p 8888:8888 photoconnect/config-service:dev
# =============================================================================

FROM maven:3.9-eclipse-temurin-21-alpine AS builder
WORKDIR /workspace
COPY pom.xml /workspace/pom.xml
COPY config-service /workspace/config-service
RUN mvn -B -f /workspace/config-service/pom.xml -DskipTests package

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S app && adduser -S -G app app
USER app
WORKDIR /app
COPY --from=builder /workspace/config-service/target/*.jar /app/app.jar
# Bake the config repo into the image. NativeEnvironmentRepository reads from
# ${user.dir}/config-repo (see application-native.properties), and our WORKDIR
# is /app, so the served path is /app/config-repo. Keep the trailing slash —
# we want the DIRECTORY's contents, not the directory itself.
COPY --chown=app:app config-repo/ /app/config-repo/
EXPOSE 8888
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
