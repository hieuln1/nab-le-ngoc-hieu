FROM openjdk:8-jdk-alpine
EXPOSE 8081
ARG JAR_FILE=build/libs/third-party-service.jar
ADD ${JAR_FILE} app.jar
ENTRYPOINT ["java","-jar","/app.jar"]