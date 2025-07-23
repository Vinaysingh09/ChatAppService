FROM openjdk:17-jdk-slim

WORKDIR /app

# Copy Maven files for dependency caching
COPY pom.xml .
COPY src ./src

# Install Maven
RUN apt-get update && apt-get install -y maven

# Build the application
RUN mvn clean package -DskipTests

# Create uploads directory
RUN mkdir -p uploads

# Expose port
EXPOSE 8080

# Run the application
CMD ["java", "-jar", "target/chat-application-1.0.0.jar"] 