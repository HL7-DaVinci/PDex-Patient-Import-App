FROM gradle:5.4.1-jdk8-alpine AS build
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle build --no-daemon

FROM openjdk:8-jre-alpine AS provider-smart-app
EXPOSE 8443
RUN mkdir /app
COPY --from=build /home/gradle/src/payer2provider/provider-smart-app/build/libs/*.jar /app/service.jar
ENTRYPOINT exec java $JAVA_OPTS -jar /app/service.jar

FROM openjdk:8-jre-alpine AS payer-cds-hooks-service
EXPOSE 8082
RUN mkdir /app
COPY --from=build /home/gradle/src/payer2provider/payer-cds-hooks-service/build/libs/*.jar /app/service.jar
ENTRYPOINT exec java $JAVA_OPTS -jar /app/service.jar


