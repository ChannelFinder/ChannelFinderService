# syntax=docker/dockerfile:1

FROM eclipse-temurin:17-jdk AS builder
WORKDIR /build
RUN apt-get update && apt-get install -y maven
COPY . .
RUN mvn --batch-mode --update-snapshots clean package -DskipTests

FROM eclipse-temurin:17-jre AS runner
WORKDIR /app
COPY --from=builder /build/target/ChannelFinder-*.jar ./channelfinder.jar
CMD ["java", "-jar", "/app/channelfinder.jar", "--spring.config.name=application"]
