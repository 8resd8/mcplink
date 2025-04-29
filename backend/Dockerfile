#FROM  bellsoft/liberica-openjdk-alpine:17 AS build
#LABEL authors="resd"
#ARG JAR_FILE=build/libs/mcplink-0.0.1-SNAPSHOT.jar
#COPY ${JAR_FILE} mcplink.jar
#
#ENTRYPOINT ["java", "-jar", "mcplink.jar"]
#
#EXPOSE 8081

FROM gradle:8.11.1-jdk17 AS builder
LABEL cache_buster=v8

COPY gradlew .
COPY gradle ./gradle
COPY build.gradle .
COPY settings.gradle .

COPY src ./src

RUN chmod +x gradlew
RUN ./gradlew clean build -x test

RUN ls -al build/libs/
RUN cp build/libs/mcplink-0.0.1-SNAPSHOT.jar /tmp/
RUN ls -al /tmp/

FROM bellsoft/liberica-openjdk-alpine:17

WORKDIR /app

# ARG JAR_FILE=build/libs/mcplink-0.0.1-SNAPSHOT.jar # 주석 처리 또는 제거

COPY --from=builder /tmp/mcplink-0.0.1-SNAPSHOT.jar /app/mcplink.jar

ENTRYPOINT ["java", "-jar", "/app/mcplink.jar"]

EXPOSE 8081