package ru.yandex.practicum.request.controller;

import jakarta.validation.constraints.Positive;
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
import ru.yandex.practicum.dto.request.RequestDto;
import ru.yandex.practicum.dto.request.RequestStatusUpdateDto;
import ru.yandex.practicum.dto.request.RequestStatusUpdateResultDto;
import ru.yandex.practicum.exception.EventNotFoundException;
import ru.yandex.practicum.exception.EventNotPublishedException;
import ru.yandex.practicum.exception.ParticipantLimitExceededException;
import ru.yandex.practicum.exception.RequestAlreadyExistsException;
import ru.yandex.practicum.exception.RequestNotFoundException;
import ru.yandex.practicum.exception.RequestSelfAttendException;
import ru.yandex.practicum.exception.ServiceException;
import ru.yandex.practicum.exception.UserNotFoundException;
import ru.yandex.practicum.request.service.RequestService;

import java.util.List;

@RestController
@RequestMapping("/")
@RequiredArgsConstructor
@Validated
public class RequestControllerPrivate {

    private final RequestService requestService;

    @GetMapping("/users/{userId}/requests")
    public List<RequestDto> getParticipationRequest(@PathVariable @Positive Long userId) throws UserNotFoundException, ServiceException {
        return requestService.getCurrentUserRequests(userId);
    }

    @PostMapping("/users/{userId}/requests")
    @ResponseStatus(HttpStatus.CREATED)
    public RequestDto participationRequest(@PathVariable @Positive Long userId, @RequestParam @Positive Long eventId) throws UserNotFoundException, ParticipantLimitExceededException, EventNotFoundException, RequestAlreadyExistsException, RequestSelfAttendException, EventNotPublishedException, ServiceException {
        return requestService.createRequest(userId, eventId);
    }

    @PatchMapping("/users/{userId}/requests/{requestId}/cancel")
    public RequestDto cancelParticipationRequest(@PathVariable @Positive Long userId, @PathVariable @Positive Long requestId) throws RequestNotFoundException {
        return requestService.cancelRequests(userId, requestId);
    }

    /**
     *  Получение информации о запросах на участие в событии текущего пользователя
     */
    @GetMapping("/users/{userId}/events/{eventId}/requests")
    public List<RequestDto> getRequestsByOwnerOfEvent(@PathVariable @Positive Long userId, @PathVariable @Positive Long eventId) throws EventNotFoundException, ServiceException {
        return requestService.getRequestsByOwnerOfEvent(userId, eventId);
    }

    /**
     * Изменение статуса (подтверждена, отменена) заявок на участие в событии текущего пользователя
     */
    @PatchMapping("/users/{userId}/events/{eventId}/requests")
    public RequestStatusUpdateResultDto updateRequests(@PathVariable @Positive Long userId,
                                                       @PathVariable @Positive Long eventId,
                                                       @RequestBody @Validated RequestStatusUpdateDto requestStatusUpdateDto) throws EventNotFoundException, EventNotPublishedException, ParticipantLimitExceededException, ServiceException {
        return requestService.updateRequests(userId, eventId, requestStatusUpdateDto);
    }

}
