# Step 1: Build React frontend
FROM node:22.17.1-slim AS frontend-builder
WORKDIR /app/frontend
COPY ./frontend/ ./
RUN npm install
RUN npm run build

# Step 2: Build Spring Boot backend
FROM maven:3.9.6-eclipse-temurin-21 AS backend-builder
WORKDIR /app/backend
COPY ./backend/ ./
RUN mvn clean package -DskipTests

# Step 3: Final Image with JAR and Static Files
FROM openjdk:21-jdk-slim
WORKDIR /app

# Copy Spring Boot JAR
COPY --from=backend-builder /app/backend/target/*.jar app.jar

# Copy frontend build into Spring Boot's static resources
COPY --from=frontend-builder /app/frontend/dist /app/public

# Optional: if using Spring Boot to serve frontend
ENV SPRING_RESOURCES_STATIC_LOCATIONS=file:/app/public/

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]