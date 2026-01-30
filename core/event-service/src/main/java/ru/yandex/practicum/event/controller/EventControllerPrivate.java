package ru.yandex.practicum.event.controller;

import jakarta.validation.constraints.Positive;
import jakarta.validation.groups.Default;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.dto.event.EventDto;
import ru.yandex.practicum.dto.event.EventDtoFull;
import ru.yandex.practicum.dto.validation.ValidationGroups;
import ru.yandex.practicum.event.EventService;
import ru.yandex.practicum.exception.CategoryNotFoundException;
import ru.yandex.practicum.exception.EventCanceledCantPublishException;
import ru.yandex.practicum.exception.EventDateException;
import ru.yandex.practicum.exception.EventNotFoundException;
import ru.yandex.practicum.exception.EventValidationException;
import ru.yandex.practicum.exception.ServiceException;
import ru.yandex.practicum.exception.UserNotFoundException;

import java.util.List;

@RestController
@RequestMapping(path = "/users/{userId}/events")
@RequiredArgsConstructor
@Validated
public class EventControllerPrivate {

    private final EventService eventService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EventDtoFull createEvent(@PathVariable @Positive Long userId, @RequestBody @Validated({ValidationGroups.Create.class, Default.class}) EventDto eventDto) throws UserNotFoundException, CategoryNotFoundException, EventDateException, EventValidationException, ServiceException {
        eventDto.setInitiator(userId);
        return eventService.createEvent(eventDto);
    }

    @GetMapping
    public List<EventDtoFull> findEventsByUser(@PathVariable @Positive Long userId,
                                               @RequestParam(name = "from", defaultValue = "0") int from,
                                               @RequestParam(name = "size", defaultValue = "10") int size) {

        return eventService.findEventsByUserid(userId, from, size);
    }

    @GetMapping("/{eventId}")
    public EventDtoFull findEventByUserId(@PathVariable @Positive Long userId, @PathVariable @Positive Long eventId) throws EventNotFoundException {

        return eventService.findEventByUserId(userId, eventId);
    }

    @PatchMapping("/{eventId}")
    public EventDtoFull updateEventByUserId(@PathVariable Long userId, @PathVariable Long eventId,
                                            @RequestBody @Validated(Default.class) EventDto eventDto) throws EventNotFoundException, EventDateException, EventCanceledCantPublishException {
        eventDto.setInitiator(userId);
        eventDto.setId(eventId);
        return eventService.updateEventByUserId(eventDto);
    }

}
