package ru.yandex.practicum.client.fallback;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.api.RequestClient;
import ru.yandex.practicum.dto.BooleanResponseDto;
import ru.yandex.practicum.dto.request.ConfirmedRequestsCountDto;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
public class RequestClientFallback implements RequestClient {

    @Override
    public ConfirmedRequestsCountDto getConfirmedRequestsCount(Long eventId) {
        log.warn("RequestClient fallback triggered for getConfirmedRequestCount with eventId: {}", eventId);

        return new ConfirmedRequestsCountDto(eventId, 0L);
    }

    @Override
    public BooleanResponseDto checkUserParticipation(Long userId, Long eventId) {
        log.warn("RequestClient fallback triggered for checkUserParticipation, userId: {}, eventId: {}",
                userId, eventId);

        return BooleanResponseDto.builder()
                .result(false)
                .message("Request-service is temporary unavailable")
                .build();
    }

    @Override
    public List<ConfirmedRequestsCountDto> getConfirmedRequests(List<Long> eventIds) {
        log.warn("RequestClient fallback triggered for getConfirmedRequests with eventIds: {}", eventIds);

        if (eventIds == null || eventIds.isEmpty()) {
            return Collections.emptyList();
        }

        return eventIds.stream()
                .map(eventId -> new ConfirmedRequestsCountDto(eventId, 0L))
                .collect(Collectors.toList());
    }
}
