package ru.yandex.practicum.request.service;

import ru.yandex.practicum.dto.BooleanResponseDto;
import ru.yandex.practicum.dto.request.ConfirmedRequestsCountDto;
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

import java.util.List;

public interface RequestService {

    List<RequestDto> getRequestsByOwnerOfEvent(Long userId, Long eventId) throws EventNotFoundException, ServiceException;

    RequestStatusUpdateResultDto updateRequests(Long userId, Long eventId, RequestStatusUpdateDto requestStatusUpdateDto) throws EventNotFoundException, EventNotPublishedException, ParticipantLimitExceededException, ServiceException;

    RequestDto createRequest(Long userId, Long eventId) throws EventNotFoundException, RequestAlreadyExistsException, ParticipantLimitExceededException, RequestSelfAttendException, EventNotPublishedException, ru.yandex.practicum.exception.UserNotFoundException, ServiceException;

    List<RequestDto> getCurrentUserRequests(Long userId) throws UserNotFoundException, ServiceException;

    RequestDto cancelRequests(Long userId, Long requestId) throws RequestNotFoundException;

    BooleanResponseDto checkUserParticipation(Long userId, Long eventId);

    List<ConfirmedRequestsCountDto> getConfirmedRequests(List<Long> eventIds);

    ConfirmedRequestsCountDto getConfirmedRequestsCount(Long eventId);

}
