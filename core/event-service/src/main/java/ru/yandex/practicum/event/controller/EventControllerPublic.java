package ru.yandex.practicum.event.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.dto.event.EventDtoShort;
import ru.yandex.practicum.event.EventService;
import ru.yandex.practicum.dto.event.EventDtoFull;
import ru.yandex.practicum.dto.event.EventFilterPublic;
import ru.yandex.practicum.exception.EventDateException;
import ru.yandex.practicum.exception.EventNotFoundException;
import ru.yandex.practicum.exception.FilterValidationException;
import ru.yandex.practicum.exception.ServiceException;
import ru.yandex.practicum.exception.UserNotFoundException;

import java.util.List;

@RestController
@RequestMapping(path = "/events")
@RequiredArgsConstructor
@Validated
public class EventControllerPublic {

    private final static String X_EWM_USER_ID = "X-EWM-USER-ID";
    private final EventService eventService;

    @GetMapping
    public List<EventDtoFull> findEvents(@Valid EventFilterPublic eventFilterPublic, HttpServletRequest request) throws FilterValidationException, EventDateException {
        return eventService.findEvents(eventFilterPublic, request);
    }

    @GetMapping("/{eventId}")
    public EventDtoFull findEvenById(@PathVariable("eventId") @Positive Long eventId, @RequestHeader(X_EWM_USER_ID) Long userId) throws EventNotFoundException {
        return eventService.findEventById(eventId, userId);
    }

    @GetMapping("/recommendations")
    public List<EventDtoShort> getRecommendation(@RequestParam Long max, @RequestHeader(X_EWM_USER_ID) Long userId) {
        return eventService.getRecommendations(max, userId);
    }

    @PutMapping("/{eventId}/like")
    public void likeEvent(@PathVariable Long eventId, @RequestHeader(X_EWM_USER_ID) Long userId) throws EventNotFoundException, UserNotFoundException, ServiceException {
        eventService.addLike(eventId, userId);
    }
}
