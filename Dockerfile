# Use OpenJDK base image
FROM eclipse-temurin:17-jdk-jammy

WORKDIR /app

# Copy source code
COPY . .

# Make gradlew executable
RUN chmod +x ./gradlew

# Build the JAR
RUN ./gradlew bootJar -x test

# Expose default port
EXPOSE 8080

# Run the Spring Boot app
CMD ["java", "-jar", "build/libs/gatekeeper-0.0.1-SNAPSHOT.jar"]

