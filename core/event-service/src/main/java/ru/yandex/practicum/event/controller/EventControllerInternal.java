package ru.yandex.practicum.event.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.api.EventClient;
import ru.yandex.practicum.dto.BooleanResponseDto;
import ru.yandex.practicum.dto.event.EventDtoFull;
import ru.yandex.practicum.event.EventService;
import ru.yandex.practicum.exception.EventNotFoundException;

@RestController
@RequestMapping("/internal/events")
@RequiredArgsConstructor
@Validated
public class EventControllerInternal implements EventClient {

    private final EventService eventService;

    @Override
    @GetMapping("/{eventId}")
    public EventDtoFull getEvent(Long eventId) throws EventNotFoundException {
        return eventService.findEventById(eventId);
    }

    @Override
    @GetMapping("/{eventId}/exists")
    public BooleanResponseDto existsEvent(Long eventId) {
        return eventService.existsEvent(eventId);
    }

    @Override
    @GetMapping("/{eventId}/initiator/{userId}")
    public EventDtoFull getEventByInitiator(Long eventId, Long userId) throws EventNotFoundException {
        return eventService.findEventByUserId(eventId, userId);
    }
}
