package ru.yandex.practicum.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.ewm.stats.avro.UserActionAvro;
import ru.yandex.practicum.service.UserActionService;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserActionListener {

    private final UserActionService userActionService;

    @KafkaListener(
            topics = "${kafka.topics.user-action-topic}",
            containerFactory = "kafkaListenerContainerFactoryUserAction"
    )
    public void onMessage(
            @Payload UserActionAvro message,
            Acknowledgment acknowledgment,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        log.debug("UserActionListener. Receive message: topic={}, partition={}, offset={}, userId={}, actionType={}",
                topic, partition, offset,
                message.getUserId(), message.getActionType());

        try {
            userActionService.saveUserAction(message);

            acknowledgment.acknowledge();

            log.debug("UserActionListener. Message confirmed: topic={}, partition={}, offset={}",
                    topic, partition, offset);

        } catch (Exception e) {
            log.error("UserActionListener. Message process error: topic={}, partition={}, offset={}: {}",
                    topic, partition, offset, e.getMessage(), e);

        }
    }
}
