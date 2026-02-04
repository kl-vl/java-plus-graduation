FROM amazoncorretto:21-alpine
RUN apk add --no-cache curl
COPY target/*.jar user-service.jar
ENTRYPOINT ["java", "-jar", "user-service.jar"]