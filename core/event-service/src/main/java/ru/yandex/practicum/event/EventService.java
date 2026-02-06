package ru.yandex.practicum.event;

import jakarta.servlet.http.HttpServletRequest;
import ru.yandex.practicum.dto.BooleanResponseDto;
import ru.yandex.practicum.dto.event.EventDto;
import ru.yandex.practicum.dto.event.EventDtoFull;
import ru.yandex.practicum.dto.event.EventDtoShort;
import ru.yandex.practicum.dto.event.EventFilterAdmin;
import ru.yandex.practicum.dto.event.EventFilterPublic;
import ru.yandex.practicum.exception.CategoryNotFoundException;
import ru.yandex.practicum.exception.EventAlreadyPublishedException;
import ru.yandex.practicum.exception.EventCanceledCantPublishException;
import ru.yandex.practicum.exception.EventDateException;
import ru.yandex.practicum.exception.EventNotFoundException;
import ru.yandex.practicum.exception.EventValidationException;
import ru.yandex.practicum.exception.FilterValidationException;
import ru.yandex.practicum.exception.UserNotFoundException;
import ru.yandex.practicum.exception.ServiceException;

import java.util.List;

public interface EventService {
    // Admin
    List<EventDtoFull> findEventsByUsers(EventFilterAdmin eventFilter) throws FilterValidationException, EventDateException;

    EventDtoFull updateEventById(EventDto eventDto) throws EventNotFoundException, EventValidationException, EventDateException, EventAlreadyPublishedException, EventCanceledCantPublishException;

    // Private
    EventDtoFull createEvent(EventDto eventDto) throws EventValidationException, CategoryNotFoundException, UserNotFoundException, EventDateException, ServiceException;

    List<EventDtoFull> findEventsByUserid(Long userId, int from, int size);

    EventDtoFull findEventByUserId(Long userId, Long eventId) throws EventNotFoundException;

    EventDtoFull updateEventByUserId(EventDto eventDto) throws EventNotFoundException, EventDateException, EventCanceledCantPublishException;

    //Public
    List<EventDtoFull> findEvents(EventFilterPublic eventFilter, HttpServletRequest request) throws FilterValidationException, EventDateException;

    EventDtoFull findEventById(Long eventId, HttpServletRequest request) throws EventNotFoundException;

    EventDtoFull findEventById(Long eventId, Long userId) throws EventNotFoundException;

    List<EventDtoShort> getRecommendations(Long max, Long userId);

    void addLike(Long eventId, Long userId) throws EventNotFoundException, ServiceException, UserNotFoundException;

    // Internal
    EventDtoFull findEventById(Long eventId) throws EventNotFoundException;

    BooleanResponseDto existsEvent(Long eventId);

}
