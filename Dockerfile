# syntax=docker/dockerfile:1

FROM eclipse-temurin:25-jdk AS builder
WORKDIR /build
COPY mvnw ./
COPY .mvn .mvn
RUN ./mvnw --batch-mode --version
COPY . .
RUN ./mvnw --batch-mode --update-snapshots clean package -DskipTests

FROM eclipse-temurin:25-jre AS runner
WORKDIR /app
COPY --from=builder /build/target/ChannelFinder-*.jar ./channelfinder.jar
CMD ["java", "-jar", "/app/channelfinder.jar", "--spring.config.name=application"]
