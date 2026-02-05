package ru.yandex.practicum.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.yandex.practicum.service.EventSimilarityService;

@Component
@RequiredArgsConstructor
@Slf4j
public class EventSimilarityListener {

    private final EventSimilarityService service;

    @KafkaListener(
            topics = "${kafka.topics.similarity-topic}",
            containerFactory = "kafkaListenerContainerFactorySimilarity"
    )
    public void onMessage(
            @Payload EventSimilarityAvro message,
            Acknowledgment acknowledgment,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        log.info("EventSimilarityListener. Receive message: topic={}, partition={}, offset={}, eventA={}, seventB={}",
                topic, partition, offset, message.getEventA(), message.getEventB());

        try {
            service.saveSimilarity(message);

            acknowledgment.acknowledge();

            log.debug("EventSimilarityListener. Message confirmed: topic={}, partition={}, offset={}", topic, partition, offset);

        } catch (Exception e) {
            log.error("EventSimilarityListener. Message process error: topic={}, partition={}, offset={}]: {}",
                    topic, partition, offset, e.getMessage(), e);
        }
    }

}
