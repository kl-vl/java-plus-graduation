package ru.yandex.practicum.runner;

import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.yandex.practicum.ewm.stats.avro.UserActionAvro;
import ru.yandex.practicum.service.AggregatorService;

import java.util.List;

@Component
@Slf4j
public class AggregatorStarter {
    private final AggregatorService aggregatorService;
    private final KafkaTemplate<String, SpecificRecordBase> kafkaTemplate;

    private AggregatorService createAggregatorService() {
        return new AggregatorService();
    }

    @Autowired
    public AggregatorStarter(KafkaTemplate<String, SpecificRecordBase> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
        this.aggregatorService = createAggregatorService();
    }

    @Value("${kafka.topics.similarity-topic}")
    private String topic;

    @KafkaListener(
            topics = "${kafka.topics.user-action-topic}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onMessage(UserActionAvro userActionAvro, Acknowledgment acknowledgment) {
        try {
            log.debug("Aggregator. Event received: {}", userActionAvro);

            List<EventSimilarityAvro> similarities =
                    aggregatorService.calculateSimilarity(userActionAvro);

            similarities.forEach(similarity -> {
                        kafkaTemplate.send(topic, similarity);
                        log.debug("Aggregator. Sending to topic {} message: {}", topic, similarity.toString());
                    }
            );

            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("Aggregator. Error processing UserAction", e);
        }
    }

}
