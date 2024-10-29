# Stage 1: Development stage
FROM eclipse-temurin:17-jdk AS developer

# Install Maven and Git for development purposes
RUN apt-get update && \
    apt-get install -y maven git && \
    apt-get clean

# Set the working directory for development
WORKDIR /workspace

# Optionally, start an interactive shell for development
CMD ["/bin/bash"]

# Stage 2: Build stage
FROM eclipse-temurin:17-jdk AS builder

# Copy the application code from the developer workspace or local context
COPY . /workspace
WORKDIR /workspace

# Run Maven to clean and build the application JAR
RUN mvn clean install

# Stage 3: Production deployment stage
FROM eclipse-temurin:17-jre AS production

# Copy only the built JAR from the builder stage
COPY --from=builder /workspace/target/ChannelFinder-*.jar /channelfinder/ChannelFinder-*.jar

# Set the CMD to run the application in production mode
CMD ["java", "-jar", "/channelfinder/ChannelFinder-*.jar", "--spring.config.name=application"]
