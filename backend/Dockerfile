# Stage 1: Build the application
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Copy the pom.xml and download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy the source code and build the application
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Create a lightweight image for runtime
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copy the built jar from the build stage
COPY --from=build /app/target/mini-dmart-1.0.0.jar app.jar

# Spring Boot binds to 0.0.0.0 by default.
# Render dynamically sets the PORT environment variable.
ENV PORT=8080
EXPOSE ${PORT}

# Run the jar file, injecting the PORT environment variable
ENTRYPOINT ["sh", "-c", "java -jar -Dserver.port=${PORT} app.jar"]
