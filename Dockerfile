# syntax=docker/dockerfile:1
# ==========================================
# Stage 1: Build Frontend (React/Vite)
# ==========================================
FROM node:24-alpine AS frontend-build
WORKDIR /app/frontend

# Pinned so the lockfile format always matches the pnpm that reads it.
RUN corepack enable && corepack prepare pnpm@12.4.2 --activate
COPY frontend/package.json frontend/pnpm-lock.yaml frontend/pnpm-workspace.yaml ./
RUN --mount=type=cache,target=/root/.local/share/pnpm/store \
    pnpm install --frozen-lockfile
COPY frontend/ ./

# Pass the Razorpay Key to Vite during the build
ARG VITE_RAZORPAY_KEY_ID
ENV VITE_RAZORPAY_KEY_ID=$VITE_RAZORPAY_KEY_ID

# Output goes to /app/frontend/dist
RUN pnpm run build

# ==========================================
# Stage 2: Build Backend (Spring Boot)
# ==========================================
FROM maven:3.9-eclipse-temurin-25 AS backend-build
WORKDIR /app/backend

# Resolve dependencies in their own layer so a source-only change skips the download.
COPY backend/pom.xml ./
RUN --mount=type=cache,target=/root/.m2 mvn -B -q dependency:go-offline

COPY backend/src ./src

# Copy the React build into Spring Boot's static folder BEFORE packaging
COPY --from=frontend-build /app/frontend/dist ./src/main/resources/static/

# Package the JAR, then unpack it: an extracted jar starts faster than a fat jar
# and is what the JVM's AOT cache needs (it only works with plain classpath jars).
RUN --mount=type=cache,target=/root/.m2 mvn -B -q package -DskipTests \
    && cp target/*.jar app.jar \
    && java -Djarmode=tools -jar app.jar extract --destination /app/extracted

# ==========================================
# Stage 3: Production Runtime
# ==========================================
FROM eclipse-temurin:25-jre
WORKDIR /app

COPY --from=backend-build /app/extracted ./

# Training run: start the Spring context once and exit right after refresh, while
# the JVM records loaded and linked classes into app.aot. Real starts then load
# them from the cache instead of parsing jars again, which cuts boot time sharply.
# No database or Redis exists at build time, so the run uses placeholder values
# and tells Hibernate not to connect (runners like DevSeeder never execute).
# The heap here is kept small on purpose: a cache trained with a multi-GB heap uses
# a different compressed-pointer layout and the JVM refuses to start with it on a
# 512MB host. The GC must match the ENTRYPOINT for the same reason.
RUN java -XX:AOTCacheOutput=app.aot -XX:+UseSerialGC -Xmx384m \
    -Dspring.context.exit=onRefresh \
    -Dspring.jpa.hibernate.ddl-auto=none \
    -Dspring.jpa.properties.hibernate.boot.allow_jdbc_metadata_access=false \
    -Dspring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect \
    -DDB_URL=jdbc:postgresql://localhost:5432/training -DDB_USER=x -DDB_PASS=x \
    -DREDIS_URL=redis://localhost:6379 \
    -DRAZORPAY_KEY_ID=x -DRAZORPAY_KEY_SECRET=x \
    -jar app.jar

EXPOSE 8080
# MaxRAMPercentage lets the heap use most of a small container instead of the
# JVM default of 25%. InitialRAMPercentage matters too: at 512MB the default start
# heap (8MB) is too small to load the cache's stored objects and the JVM exits.
# SerialGC suits the 1-CPU, small-memory boxes this runs on.
ENTRYPOINT ["java", "-XX:AOTCache=app.aot", "-XX:+UseSerialGC", "-XX:InitialRAMPercentage=25", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
