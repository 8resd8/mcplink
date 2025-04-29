#FROM  bellsoft/liberica-openjdk-alpine:17 AS build
#LABEL authors="resd"
#ARG JAR_FILE=build/libs/mcplink-0.0.1-SNAPSHOT.jar
#COPY ${JAR_FILE} mcplink.jar
#
#ENTRYPOINT ["java", "-jar", "mcplink.jar"]
#
#EXPOSE 8081

FROM gradle:8.11.1-jdk17 AS builder

COPY gradlew .
COPY gradle ./gradle
COPY build.gradle .
COPY settings.gradle .

COPY src ./src

RUN chmod +x gradlew

RUN ./gradlew clean build -x test

FROM bellsoft/liberica-openjdk-alpine:17

ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} mcplink.jar

ENTRYPOINT ["java", "-jar", "mcplink.jar"]

EXPOSE 8081