# Stage 1: build del jar
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY . .
RUN chmod +x gradlew && ./gradlew clean bootJar -x test --no-daemon

# Stage 2: runtime
FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app
COPY --from=builder /app/build/libs/carrier_management_service-0.0.1-SNAPSHOT.jar app.jar
ARG GOOGLE_MAPS_API_KEY
ENV GOOGLE_MAPS_API_KEY=${GOOGLE_MAPS_API_KEY}
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]