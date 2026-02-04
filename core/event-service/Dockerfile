FROM amazoncorretto:21-alpine
RUN apk add --no-cache curl
COPY target/*.jar event-service.jar
ENTRYPOINT ["java", "-jar", "event-service.jar"]