package com.tutorial.sharedmodule.infra.avro;

import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import org.apache.avro.specific.SpecificRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class AvroPayloadSerializer {

    private final KafkaAvroSerializer serializer;

    public AvroPayloadSerializer(
            @Value("${spring.kafka.properties.schema.registry.url}")
            String schemaRegistryUrl) {

        Map<String, Object> config = new HashMap<>();

        config.put(
                AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG,
                schemaRegistryUrl
        );

        serializer = new KafkaAvroSerializer();

        serializer.configure(config, false);
    }

    public byte[] serialize(
            String topic,
            SpecificRecord payload) {

        return serializer.serialize(topic, payload);
    }
}