## Use official OpenJDK 17 base image
#FROM eclipse-temurin:17-jdk-jammy
#
## Set working directory
#WORKDIR /app
#
## Copy Gradle wrapper and build files to leverage build caching
#COPY gradlew .
#COPY gradle gradle
#COPY build.gradle.kts .
#COPY settings.gradle.kts .
#
## Download dependencies (will be cached if unchanged)
#RUN ./gradlew build -x test --continue || true
#
## Copy the rest of the source code
#COPY src src
#
## Build the project and create the jar
#RUN ./gradlew clean bootJar -x test
#
## Expose port your app runs on (default 8080)
#EXPOSE 8080
#
## Run the jar file
#ENTRYPOINT ["java","-jar","build/libs/gatekeeper-0.0.1-SNAPSHOT.jar"]

# Use OpenJDK base image
FROM eclipse-temurin:17-jdk-jammy

WORKDIR /app

# Copy and build
COPY . .
RUN ./gradlew bootJar -x test

EXPOSE 8080
CMD ["java", "-jar", "build/libs/gatekeeper-0.0.1-SNAPSHOT.jar"]

