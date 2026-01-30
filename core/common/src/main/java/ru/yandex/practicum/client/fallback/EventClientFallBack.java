package ru.yandex.practicum.client.fallback;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.api.EventClient;
import ru.yandex.practicum.dto.BooleanResponseDto;
import ru.yandex.practicum.dto.event.EventDtoFull;
import ru.yandex.practicum.exception.EventNotFoundException;

@Component
@Slf4j
public class EventClientFallBack implements EventClient {
    @Override
    public EventDtoFull getEvent(Long eventId) throws EventNotFoundException {
        log.warn("EventClient fallback triggered for getEvent, eventId={}", eventId);
        throw new EventNotFoundException("Event-service is temporary unavailable. eventId=" + eventId);
    }

    @Override
    public BooleanResponseDto existsEvent(Long eventId) {
        log.warn("EventClient fallback triggered for existsEvent, eventId={}", eventId);
        return BooleanResponseDto.builder()
                .result(false)
                .message("Event-service is temporary unavailable")
                .build();
    }

    @Override
    public EventDtoFull getEventByInitiator(Long eventId, Long userId) throws EventNotFoundException {
        log.warn("Fallback: getEventByInitiator(eventId={}, userId={})", eventId, userId);

        throw new EventNotFoundException(
                String.format("Event service unavailable. EventId: %d, UserId: %d", eventId, userId)
        );
    }
    
}
