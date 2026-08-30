# Root Dockerfile for Render.com deployment
FROM maven:3.9.8-eclipse-temurin-21 AS build
WORKDIR /app

# Copy pom.xml and download dependencies
COPY backend/pom.xml ./pom.xml
RUN mvn dependency:go-offline -B

# Copy backend source code and build executable jar
COPY backend/src ./src
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create upload directory for images
RUN mkdir -p uploads

# Copy built jar from build stage
COPY --from=build /app/target/priceiq-backend-1.0.0-SNAPSHOT.jar app.jar

# Expose port (Render automatically sets $PORT)
ENV PORT=10000
EXPOSE 10000

# Run Spring Boot app
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
