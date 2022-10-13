FROM maven:3-jdk-11 as maven-build
COPY ./pom.xml /bulk-data-requester/
COPY ./src /bulk-data-requester/src/
WORKDIR /bulk-data-requester/
RUN mvn clean install -DskipTests

FROM alpine:3.14
RUN apk update
RUN apk --no-cache add openjdk11
RUN apk --no-cache add bash
RUN apk --no-cache add curl

#RUN addgroup -S spring && adduser -S spring -G spring
#USER spring:spring

COPY --from=maven-build /bulk-data-requester/target/*.jar /app.jar

ENTRYPOINT ["java","-jar","/app.jar"]

