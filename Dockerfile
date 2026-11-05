
# Stage 1: Build frontend
FROM node:20-alpine AS web

WORKDIR /app/frontend

COPY frontend/package*.json ./
RUN npm ci

COPY frontend/ ./
RUN npm run build


# Stage 2: Build backend
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app

COPY backend/pom.xml ./
COPY backend/src ./src

# Copy frontend production build into Spring Boot static resources
COPY --from=web /app/frontend/dist ./src/main/resources/static

RUN mvn -q clean package -DskipTests


# Stage 3: Run application
FROM eclipse-temurin:17-jre

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

ENV JAVA_TOOL_OPTIONS="-Xmx350m"

EXPOSE 8080

CMD ["java", "-jar", "app.jar"]
