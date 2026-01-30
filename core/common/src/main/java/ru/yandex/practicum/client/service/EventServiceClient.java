package ru.yandex.practicum.client.service;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.api.EventClient;
import ru.yandex.practicum.dto.BooleanResponseDto;
import ru.yandex.practicum.dto.event.EventDtoFull;
import ru.yandex.practicum.exception.EventNotFoundException;
import ru.yandex.practicum.exception.ServiceException;

@Component
@Slf4j
@RequiredArgsConstructor
public class EventServiceClient {
    private final EventClient eventClient;

    public void validateEventExists(Long eventId) throws EventNotFoundException, ServiceException {
        log.info("Validating event existence: eventId = {}", eventId);

        try {
            BooleanResponseDto existsResponse = eventClient.existsEvent(eventId);
            if (!existsResponse.isResult()) {
                throw new EventNotFoundException(
                        String.format("Event with id %d not found. Reason: %s",
                                eventId, existsResponse.getMessage())
                );
            }
            log.info("Event validation successful: eventId = {}", eventId);

        } catch (EventNotFoundException e) {
            throw e;

        } catch (Exception e) {
            log.warn("Event-service call failed for eventId {} : {}", eventId, e.getMessage(), e);
            throw new ServiceException("Event-service unavailable.", e);
        }
    }

    public EventDtoFull fetchEventDto(Long eventId) throws EventNotFoundException, ServiceException {
        log.info("Fetching event: eventId = {}", eventId);
        try {
            EventDtoFull eventDtoFull = eventClient.getEvent(eventId);
            log.info("Event fetching successful: eventId = {}, title = {}", eventDtoFull.getId(), eventDtoFull.getTitle());
            return eventDtoFull;
        } catch (FeignException.NotFound e) {
            log.warn("Event not found: eventId = {}. Feign response: {}", eventId, e.contentUTF8());
            throw new EventNotFoundException(eventId);
        } catch (EventNotFoundException e) {
            throw new EventNotFoundException(eventId);
        } catch (Exception e) {
            log.error("Event-service call failed for eventId {}: {}", eventId, e.getMessage(), e);
            if (e.getCause() instanceof EventNotFoundException) {
                throw (EventNotFoundException) e.getCause();
            }
            throw new ServiceException("Event-service unavailable", e);
        }
    }

    public EventDtoFull fetchEventDto(Long eventId, Long userId) throws EventNotFoundException, ServiceException {
        log.info("Fetching event: eventId = {}, userId = {}", eventId, userId);
        try {
            EventDtoFull eventDtoFull = eventClient.getEventByInitiator(userId, eventId);
            log.info("Event fetching successful: eventId = {}, title = {}", eventDtoFull.getId(), eventDtoFull.getTitle());
            return eventDtoFull;
        } catch (EventNotFoundException e) {
            throw new EventNotFoundException(eventId);
        } catch (Exception e) {
            log.error("Event-service call failed for eventId {}: {}", eventId, e.getMessage(), e);
            throw new ServiceException("Event-service unavailable", e);
        }

    }

}
