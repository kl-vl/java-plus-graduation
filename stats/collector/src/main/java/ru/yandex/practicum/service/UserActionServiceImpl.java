package ru.yandex.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.grpc.user.action.UserActionProto;
import ru.yandex.practicum.mapper.UserActionMapper;

import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserActionServiceImpl implements UserActionService {

    private final KafkaTemplate<String, SpecificRecordBase> kafkaTemplate;
    @Qualifier("userActionMapper")
    private final UserActionMapper mapper;

    @Value("${kafka.topics.user-action-topic}")
    private String userActionTopic;

    @Override
    public void process(UserActionProto userAction) {
        log.info("UserActionService. UserAction: user: {}, event: {}, actionType: {}",
                userAction.getUserId(), userAction.getEventId(), userAction.getActionType());

        try {
            CompletableFuture<SendResult<String, SpecificRecordBase>> future =
                    kafkaTemplate.send(userActionTopic, mapper.mapToAvro(userAction));
            future.whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("UserActionService. Kafka error for user: {}, event: {}, actionType: {}",
                            userAction.getUserId(), userAction.getEventId(), userAction.getActionType(), ex);
                } else  {
                    log.info("UserActionService. Sent successfully for user: {}, event: {}, actionType: {}",
                            userAction.getUserId(), userAction.getEventId(), userAction.getActionType());
                }
            });
        } catch (Exception e) {
            log.error("UserActionService. Failed to send for user: {}, event: {}, actionType: {}",
                    userAction.getUserId(), userAction.getEventId(), userAction.getActionType(), e);
        }

    }
}
