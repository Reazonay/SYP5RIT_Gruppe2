FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml pom.xml
RUN mvn -B dependency:go-offline
COPY src src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
RUN useradd --system --uid 10001 restaurant
COPY --from=build --chown=restaurant:restaurant /app/target/restaurant.jar app.jar
ENV APP_PRODUCTION=true PORT=3000
USER restaurant
EXPOSE 3000
CMD ["java", "-jar", "app.jar"]
