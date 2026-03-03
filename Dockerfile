FROM eclipse-temurin:21-jdk-alpine

WORKDIR /app

COPY build/libs/carrier_management_service-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080
EXPOSE 8000

ENTRYPOINT ["java","-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:8000","-jar","app.jar"]