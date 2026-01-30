package ru.yandex.practicum.client.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.api.RequestClient;
import ru.yandex.practicum.dto.BooleanResponseDto;
import ru.yandex.practicum.dto.request.ConfirmedRequestsCount;
import ru.yandex.practicum.dto.request.ConfirmedRequestsCountDto;
import ru.yandex.practicum.exception.EventNotFoundException;
import ru.yandex.practicum.exception.ServiceException;

import java.util.Collections;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class RequestServiceClient {

    private final RequestClient requestClient;

    public boolean checkUserParticipation(Long userId, Long eventId)
            throws EventNotFoundException, ServiceException {

        log.info("Checking user participation: userId = {}, eventId = {}", userId, eventId);

        try {
            BooleanResponseDto response = requestClient.checkUserParticipation(userId, eventId);

            if (!response.isResult()) {
                throw new EventNotFoundException(
                        String.format("User with id %d is not participating in event %d",
                                userId, eventId)
                );
            }

            log.info("User participation confirmed: userId = {}, eventId = {}", userId, eventId);
            return true;

        } catch (EventNotFoundException e) {
            throw e;

        } catch (Exception e) {
            log.warn("Request-service call failed for userId={}, eventId={}: {}",
                    userId, eventId, e.getMessage(), e);
            throw new ServiceException("Request-service unavailable.", e);
        }
    }

    public Long getConfirmedRequestCount(Long eventId) {
        log.debug("Getting confirmed request count with fallback for eventId = {}", eventId);

        try {
            ConfirmedRequestsCount count = requestClient.getConfirmedRequestsCount(eventId);
            return count.getCount();

        } catch (Exception e) {
            log.warn("Request-service call failed, returning 0: {}", e.getMessage());
            return 0L;
        }
    }


    public List<ConfirmedRequestsCountDto> getConfirmedRequestsList(List<Long> eventIds) {
        log.debug("Getting confirmed requests list for {} eventIds",
                eventIds != null ? eventIds.size() : 0);

        try {
            return requestClient.getConfirmedRequests(eventIds);

        } catch (Exception e) {
            log.warn("Request-service call failed, returning empty list: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

}
