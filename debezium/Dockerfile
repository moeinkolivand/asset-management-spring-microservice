FROM maven:3.9.9-eclipse-temurin-17 AS avro-dependencies

ARG CONFLUENT_VERSION=8.0.0

WORKDIR /build

COPY pom.xml .

RUN mvn -q dependency:copy-dependencies \
    -DincludeScope=runtime \
    -DoutputDirectory=/build/lib


FROM quay.io/debezium/connect:3.0

USER root

RUN mkdir -p /kafka/connect/confluent-avro

COPY --from=avro-dependencies /build/lib/*.jar \
    /kafka/connect/confluent-avro/

USER kafka