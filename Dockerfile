# Stage 1: Development stage
FROM eclipse-temurin:17-jdk AS developer

# Install any development tools you need, for example, Maven, Git, etc.
RUN apt-get update && \
    apt-get install -y maven git && \
    apt-get clean

# Set the working directory for development
WORKDIR /workspace

# Optionally, you can clone or copy the source code if needed (e.g., for devcontainers)
# If using a local dev environment, you might mount your code with volumes instead.
# Example:
# COPY . /workspace

# Open a shell for interactive development
CMD ["/bin/bash"]

# Stage 2: Production deployment stage
FROM eclipse-temurin:17-jre AS production

# Copy the JAR from the build context
COPY target/ChannelFinder-*.jar /channelfinder/ChannelFinder-*.jar

# Set the CMD to run the application in production mode
CMD ["java", "-jar", "/channelfinder/ChannelFinder-*.jar", "--spring.config.name=application"]
