FROM amazoncorretto:21-alpine
RUN apk add --no-cache curl \
COPY target/*.jar request-service.jar
ENTRYPOINT ["java", "-jar", "request-service.jar"]