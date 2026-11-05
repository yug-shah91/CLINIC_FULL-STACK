FROM node:20 AS web
WORKDIR /app
COPY frontend ./frontend
COPY backend/src/main/resources ./backend/src/main/resources
WORKDIR /app/frontend
RUN npm ci && npm run build

FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY backend/pom.xml .
COPY backend/src ./src
COPY --from=web /app/backend/src/main/resources/static ./src/main/resources/static
RUN mvn -q package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
ENV JAVA_TOOL_OPTIONS="-Xmx350m"
EXPOSE 8080
CMD ["java","-jar","app.jar"]