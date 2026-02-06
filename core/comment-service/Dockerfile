FROM amazoncorretto:21-alpine
RUN apk add --no-cache curl
COPY target/*.jar comment-service.jar
ENTRYPOINT ["java", "-jar", "comment-service.jar"]