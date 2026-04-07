# ==========================================
# Stage 1: Build Frontend (React/Vite)
# ==========================================
FROM node:20-alpine AS frontend-build
WORKDIR /app/frontend

# Install dependencies and build
COPY frontend/package*.json ./
RUN npm install
COPY frontend/ ./

# Pass the Razorpay Key to Vite during the build
ARG VITE_RAZORPAY_KEY_ID
ENV VITE_RAZORPAY_KEY_ID=$VITE_RAZORPAY_KEY_ID

# Output goes to /app/frontend/dist
RUN npm run build

# ==========================================
# Stage 2: Build Backend (Spring Boot)
# ==========================================
FROM maven:3.9.6-eclipse-temurin-21 AS backend-build
WORKDIR /app/backend

COPY backend/pom.xml ./
COPY backend/src ./src

# Copy the React build into Spring Boot's static folder BEFORE packaging
COPY --from=frontend-build /app/frontend/dist ./src/main/resources/static/

# Package the JAR
RUN mvn clean package -DskipTests

# ==========================================
# Stage 3: Production Runtime
# ==========================================
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy the final "Fat JAR"
COPY --from=backend-build /app/backend/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
