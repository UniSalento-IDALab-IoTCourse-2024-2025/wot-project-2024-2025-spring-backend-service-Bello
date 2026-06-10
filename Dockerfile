FROM eclipse-temurin:21-jdk-alpine

WORKDIR /app

COPY build/libs/carrier_management_service-0.0.1-SNAPSHOT.jar app.jar

ARG GOOGLE_MAPS_API_KEY
ENV GOOGLE_MAPS_API_KEY=${GOOGLE_MAPS_API_KEY}

EXPOSE 8080

ENTRYPOINT ["java","-jar","app.jar"]