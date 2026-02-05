package ru.yandex.practicum.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafka
@Slf4j
public class AnalyzerKafkaConfig {

    @Value("${kafka.bootstrap-server}")
    private String bootstrapServer;

    @Value("${kafka.key-deserializer}")
    private String keyDeserializer;

    @Value("${kafka.analyzer-deserializer}")
    private String userActionDeserializer;

    @Value("${kafka.analyzer-event-similarity-deserializer}")
    private String similarityDeserializer;

    @Value("${kafka.analyzer-group-user-action}")
    private String userGroupId;

    @Value("${kafka.analyzer-group-similarity}")
    private String similarityGroupId;


    private Map<String, Object> commonConsumerConfigs() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServer);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, 0);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 100);
        props.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, 1);
        props.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, 500);
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 30000);
        props.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 10000);
        return props;
    }

    private ConsumerFactory<String, SpecificRecordBase> createConsumerFactory(
            String groupId, String valueDeserializer) {

        Map<String, Object> props = new HashMap<>(commonConsumerConfigs());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, keyDeserializer);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, valueDeserializer);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);

        return new DefaultKafkaConsumerFactory<>(props);
    }

    private ConcurrentKafkaListenerContainerFactory<String, SpecificRecordBase> createContainerFactory(
            ConsumerFactory<String, SpecificRecordBase> consumerFactory) {

        ConcurrentKafkaListenerContainerFactory<String, SpecificRecordBase> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(consumerFactory);
        factory.setConcurrency(Math.max(1, Runtime.getRuntime().availableProcessors() / 2));

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
                (record, exception) -> log.error("Failed to process message from topic {}: partition={}, offset={}, error={}",
                        record.topic(), record.partition(), record.offset(), exception.getMessage(), exception),
                new FixedBackOff(1000L, 2)
        );
        factory.setCommonErrorHandler(errorHandler);

        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);

        return factory;
    }

    // User Action Consumer
    @Bean
    public ConsumerFactory<String, SpecificRecordBase> userActionConsumerFactory() {
        return createConsumerFactory(userGroupId, userActionDeserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, SpecificRecordBase>
    kafkaListenerContainerFactoryUserAction() {
        return createContainerFactory(userActionConsumerFactory());
    }

    // Similarity Consumer
    @Bean
    public ConsumerFactory<String, SpecificRecordBase> similarityConsumerFactory() {
        return createConsumerFactory(similarityGroupId, similarityDeserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, SpecificRecordBase>
    kafkaListenerContainerFactorySimilarity() {
        return createContainerFactory(similarityConsumerFactory());
    }


}
