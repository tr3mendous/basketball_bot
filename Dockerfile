FROM maven:3.8.3-openjdk-17 AS build
COPY . .
RUN mvn clean install -DskipTests

FROM eclipse-temurin:17-jdk
COPY --from=build /target/basketballbot-0.0.1-SNAPSHOT.jar basketball-bot.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/basketball-bot.jar"]