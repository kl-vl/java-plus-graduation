package ru.yandex.practicum.event.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.event.EventService;
import ru.yandex.practicum.dto.event.EventDtoFull;
import ru.yandex.practicum.dto.event.EventFilterPublic;
import ru.yandex.practicum.exception.EventDateException;
import ru.yandex.practicum.exception.EventNotFoundException;
import ru.yandex.practicum.exception.FilterValidationException;

import java.util.List;

@RestController
@RequestMapping(path = "/events")
@RequiredArgsConstructor
@Validated
public class EventControllerPublic {

    private final EventService eventService;

    @GetMapping
    public List<EventDtoFull> findEvents(@Valid EventFilterPublic eventFilterPublic, HttpServletRequest request) throws FilterValidationException, EventDateException {
        return eventService.findEvents(eventFilterPublic, request);
    }

    @GetMapping("/{eventId}")
    public EventDtoFull findEvenById(@PathVariable("eventId") @Positive Long eventId, HttpServletRequest request) throws EventNotFoundException {
        return eventService.findEventById(eventId, request);
    }
}
