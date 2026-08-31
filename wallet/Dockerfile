FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app

COPY pom.xml .

COPY shared-contracts/pom.xml shared-contracts/pom.xml
COPY shared-module/pom.xml shared-module/pom.xml
COPY eureka/pom.xml eureka/pom.xml
COPY gateway/pom.xml gateway/pom.xml
COPY user/pom.xml user/pom.xml
COPY wallet/pom.xml wallet/pom.xml
COPY transaction/pom.xml transaction/pom.xml

COPY shared-contracts/src shared-contracts/src
COPY shared-module/src shared-module/src
COPY wallet/src wallet/src

RUN mvn clean package -pl wallet -am -DskipTests


FROM eclipse-temurin:17-jre

WORKDIR /app

COPY --from=build /app/wallet/target/wallet-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]