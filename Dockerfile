# syntax=docker/dockerfile:1

# ---------------------------------------------------------------------------
# Build stage: compile and package the Spring Boot app, then explode the
# layered jar so the runtime image caches dependencies separately from code.
# ---------------------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

# Resolve dependencies first; this layer is reused until pom.xml changes.
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -B -ntp dependency:go-offline

# Compile + package (tests run in CI, not in the image build).
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B -ntp clean package -DskipTests

# Normalise the name to app.jar, then split it into layers
# (dependencies / spring-boot-loader / snapshot-dependencies / application).
RUN cp target/auralis-backend.jar target/app.jar \
 && java -Djarmode=tools -jar target/app.jar extract --layers --destination target/extracted

# ---------------------------------------------------------------------------
# Runtime stage: JRE + ffmpeg only. glibc base (jammy) so the AWS SDK CRT
# native library loads; ffmpeg/ffprobe are required for asset analysis,
# waveform generation and rendering.
# ---------------------------------------------------------------------------
FROM eclipse-temurin:21-jre-jammy AS runtime

RUN apt-get update \
 && apt-get install -y --no-install-recommends ffmpeg curl tini \
 && rm -rf /var/lib/apt/lists/*

# Unprivileged user + a writable scratch dir for in-progress renders.
RUN useradd --system --create-home --home-dir /app --shell /usr/sbin/nologin auralis \
 && mkdir -p /app/work \
 && chown -R auralis:auralis /app
WORKDIR /app
USER auralis

# Copy layers least-frequently-changed first for better image-layer caching.
COPY --from=build --chown=auralis:auralis /workspace/target/extracted/dependencies/ ./
COPY --from=build --chown=auralis:auralis /workspace/target/extracted/spring-boot-loader/ ./
COPY --from=build --chown=auralis:auralis /workspace/target/extracted/snapshot-dependencies/ ./
COPY --from=build --chown=auralis:auralis /workspace/target/extracted/application/ ./

ENV SPRING_PROFILES_ACTIVE=prod \
    MEDIA_WORK_DIR=/app/work \
    JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"

EXPOSE 8080

HEALTHCHECK --interval=15s --timeout=5s --start-period=90s --retries=10 \
  CMD curl -fsS http://localhost:8080/actuator/health | grep -q '"status":"UP"' || exit 1

# tini as PID 1 for correct signal handling / zombie reaping.
ENTRYPOINT ["/usr/bin/tini", "--", "sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
