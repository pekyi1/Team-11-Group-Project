# =============================================================================
# ServiceHub Backend — Multi-Stage Dockerfile
# =============================================================================
# Stage 1: Build (Maven + JDK 17)
# Stage 2: Runtime (JRE 17 slim — no build tools in final image)
#
# Uses Debian-based images (not Alpine) for cross-platform compatibility
# with both amd64 (Linux) and arm64 (Apple Silicon Mac) architectures.
# =============================================================================

# --- Stage 1: Build ---
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app

# Copy pom.xml first — Docker caches this layer so dependencies aren't
# re-downloaded on every code change (only when pom.xml changes)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build
COPY src ./src
RUN mvn package -DskipTests -B

# --- Stage 2: Runtime ---
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# Create non-root user for security — never run containers as root
RUN groupadd -r servicehub && useradd -r -g servicehub servicehub

# Create upload directory with correct ownership
RUN mkdir -p /data/uploads && chown -R servicehub:servicehub /data/uploads

# Copy the built jar from the build stage
COPY --from=build /app/target/*.jar app.jar
RUN chown servicehub:servicehub app.jar

# Switch to non-root user
USER servicehub

# Spring Boot default port
EXPOSE 8080

# Health check — Spring Actuator health endpoint
HEALTHCHECK --interval=30s --timeout=10s --start-period=90s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# JVM settings via JAVA_OPTS env var (overridable in docker-compose)
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS:-} -jar app.jar"]
