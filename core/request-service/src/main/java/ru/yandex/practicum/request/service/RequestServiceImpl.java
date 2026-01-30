package ru.yandex.practicum.request.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.client.service.EventServiceClient;
import ru.yandex.practicum.client.service.UserServiceClient;
import ru.yandex.practicum.dto.BooleanResponseDto;
import ru.yandex.practicum.dto.event.EventDtoFull;
import ru.yandex.practicum.dto.event.enums.EventState;
import ru.yandex.practicum.dto.request.ConfirmedRequestsCount;
import ru.yandex.practicum.dto.request.ConfirmedRequestsCountDto;
import ru.yandex.practicum.dto.request.ParticipationRequestDto;
import ru.yandex.practicum.dto.request.RequestDto;
import ru.yandex.practicum.dto.request.RequestStatus;
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
import ru.yandex.practicum.request.mapper.RequestMapper;
import ru.yandex.practicum.request.model.Request;
import ru.yandex.practicum.request.repository.RequestRepository;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class RequestServiceImpl implements RequestService {
    private final RequestRepository requestRepository;
    private final RequestMapper requestMapper;
    private final UserServiceClient userServiceClient;
    private final EventServiceClient eventServiceClient;

    @Getter
    @Value("${spring.application.name}")
    private String serviceName;

    @Override
    @Transactional
    public RequestDto createRequest(Long userId, Long eventId) throws UserNotFoundException, EventNotFoundException, RequestAlreadyExistsException, ParticipantLimitExceededException, RequestSelfAttendException, EventNotPublishedException, ServiceException {
        log.info("{}. createRequest input: userId = {}, eventId = {}", getServiceName(), userId, eventId);

        userServiceClient.validateUserExists(userId);

        EventDtoFull eventDtoFull = eventServiceClient.fetchEventDto(eventId);

        if (requestRepository.existsByRequesterIdAndEventId(userId, eventId)) {
            throw new RequestAlreadyExistsException("Request for eventId = %d by userId = %d already exists".formatted(eventId, userId));
        }

        if (eventDtoFull.getInitiator()!=null && eventDtoFull.getInitiator().equals(userId)) {
            throw new RequestSelfAttendException("Cannot request to your own event");
        }

        if (eventDtoFull.getState() != EventState.PUBLISHED) {
            throw new EventNotPublishedException("Event is not published yet");
        }

        if (eventDtoFull.getParticipantLimit() != 0) {
            Long confirmedCount = requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);
            if (eventDtoFull.getParticipantLimit() > 0 && confirmedCount >= eventDtoFull.getParticipantLimit()) {
                throw new ParticipantLimitExceededException("Participant limit " + eventDtoFull.getParticipantLimit() + " exceeded for event " + eventId);
            }
            eventDtoFull.setConfirmedRequests(confirmedCount);
        }

        RequestStatus status = determineRequestStatus(eventDtoFull);

        Request request = Request.builder()
                .created(LocalDateTime.now())
                .eventId(eventId)
                .requesterId(userId)
                .status(status)
                .build();

        Request savedRequest = requestRepository.save(request);

        log.info("{}. createRequest success {}", getServiceName(), request.getId());

        return requestMapper.toDto(savedRequest);
    }

    private RequestStatus determineRequestStatus(EventDtoFull eventDtoFull) {
        // Если модерация отключена или лимит не установлен, то автоматическое подтверждение
        if (!eventDtoFull.getRequestModeration() || eventDtoFull.getParticipantLimit() == 0) {
            return RequestStatus.CONFIRMED;
        }
        return RequestStatus.PENDING;
    }

    @Override
    @Transactional
    public RequestStatusUpdateResultDto updateRequests(Long userId, Long eventId, RequestStatusUpdateDto requestStatusUpdateDto) throws EventNotFoundException, EventNotPublishedException, ParticipantLimitExceededException, ServiceException {
        //        если для события лимит заявок равен 0 или отключена пре-модерация заявок, то подтверждение заявок не требуется
        //        нельзя подтвердить заявку, если уже достигнут лимит по заявкам на данное событие (Ожидается код ошибки 409)
        //        статус можно изменить только у заявок, находящихся в состоянии ожидания (Ожидается код ошибки 409)
        //        если при подтверждении данной заявки, лимит заявок для события исчерпан, то все неподтверждённые заявки необходимо отклонить
        log.info("{}. updateRequests input: userId = {}, eventId = {}, RequestStatusUpdateDto = {}", getServiceName(), userId, eventId, requestStatusUpdateDto);

        EventDtoFull eventDtoFull = eventServiceClient.fetchEventDto(eventId, userId);

        if (!eventDtoFull.getRequestModeration() && requestStatusUpdateDto == null) {
            return new RequestStatusUpdateResultDto();
        }

        if (eventDtoFull.getState() != EventState.PUBLISHED) {
            throw new EventNotPublishedException("Event is not published");
        }

        Long confirmedCount = requestRepository.countConfirmedRequests(eventId);
        if (eventDtoFull.getParticipantLimit() > 0 && confirmedCount >= eventDtoFull.getParticipantLimit()) {
            throw new ParticipantLimitExceededException("Participant limit exceeded");
        }

        requestRepository.updateRequestsStatus(
                requestStatusUpdateDto.getRequestIds(),
                eventId,
                requestStatusUpdateDto.getStatus()
        );

        List<Request> confirmedRequests = requestRepository.findRequestsByStatus(
                requestStatusUpdateDto.getRequestIds(),
                eventId,
                RequestStatus.CONFIRMED
        );

        List<Request> rejectedRequests = requestRepository.findRequestsByStatus(
                requestStatusUpdateDto.getRequestIds(),
                eventId,
                RequestStatus.REJECTED
        );

        List<ParticipationRequestDto> confirmedRequestsList = requestMapper.toParticipationDtoList(confirmedRequests);
        List<ParticipationRequestDto> rejectedRequestsList = requestMapper.toParticipationDtoList(rejectedRequests);
        log.info("{}. updateRequests success: confirmedRequests = {}, rejectedRequests = {}", getServiceName(), confirmedRequestsList.size(), rejectedRequestsList.size());

        return RequestStatusUpdateResultDto.builder()
                .confirmedRequests(confirmedRequestsList)
                .rejectedRequests(rejectedRequestsList)
                .build();
    }

    @Override
    @Transactional
    public RequestDto cancelRequests(Long userId, Long requestId) throws RequestNotFoundException {
        log.info("{}. cancelRequests input: userId = {}, requestId = {}", getServiceName(), userId, requestId);
        Request request = requestRepository.findByIdAndRequesterId(requestId, userId)
                .orElseThrow(() -> new RequestNotFoundException("Request not found"));

        request.setStatus(RequestStatus.CANCELED);
        Request updatedRequest = requestRepository.save(request);

        log.info("{}. cancelRequests success: id = {}", getServiceName(), updatedRequest.getId());

        return requestMapper.toDto(updatedRequest);
    }

    @Override
    public List<RequestDto> getCurrentUserRequests(Long userId) throws UserNotFoundException, ServiceException {
        log.info("{}. getCurrentUserRequests input: userId = {}", getServiceName(), userId);

        userServiceClient.fetchUserDto(userId);

        List<Request> requests = requestRepository.findByRequesterId(userId);

        log.info("{}. getCurrentUserRequests success: size = {}", getServiceName(), requests.size());

        return requestMapper.toDtoList(requests);
    }

    @Override
    public List<RequestDto> getRequestsByOwnerOfEvent(Long userId, Long eventId) throws EventNotFoundException, ServiceException {
        log.info("{}. getRequestsByOwnerOfEvent input: userId = {}, eventId = {}", getServiceName(), userId, eventId);

        EventDtoFull eventDtoFull = eventServiceClient.fetchEventDto(eventId, userId);

        List<Request> requests = requestRepository.findByEventId(eventId);

        log.info("{}. getRequestsByOwnerOfEvent success: size = {}", getServiceName(), requests.size());

        return requestMapper.toDtoList(requests);
    }

    @Override
    public BooleanResponseDto checkUserParticipation(Long userId, Long eventId) {
        log.info("Request-service. Checking participation for userId: {}, eventId: {}", userId, eventId);

        boolean participation = requestRepository.existsByRequesterIdAndEventIdAndStatus(
                userId,
                eventId,
                RequestStatus.CONFIRMED
        );

        log.info("Request-service. Participation for userId: {}, eventId: {} is {}", userId, eventId, participation);

        return BooleanResponseDto.builder().result(participation).build();
    }

    @Override
    public List<ConfirmedRequestsCountDto> getConfirmedRequests(List<Long> eventIds) {
        log.info("Request-service. Getting confirmed list for eventIds={}", eventIds);

        if (eventIds == null || eventIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<ConfirmedRequestsCount> result = requestRepository
                .findConfirmedRequestsCountByEventIds(eventIds);

        Map<Long, Long> resultMap = result.stream()
                .collect(Collectors.toMap(
                        ConfirmedRequestsCount::getEventId,
                        ConfirmedRequestsCount::getCount
                ));

        return eventIds.stream()
                .map(eventId -> new ConfirmedRequestsCountDto(
                        eventId,
                        resultMap.getOrDefault(eventId, 0L)
                ))
                .collect(Collectors.toList());
    }

    @Override
    public ConfirmedRequestsCountDto getConfirmedRequestsCount(Long eventId) {
        log.info("Request-service. Getting confirmed count for eventId={}", eventId);

        List<ConfirmedRequestsCount> result = requestRepository
                .findConfirmedRequestsCountByEventIds(Collections.singletonList(eventId));

        if (!result.isEmpty()) {
            ConfirmedRequestsCount projection = result.getFirst();
            return new ConfirmedRequestsCountDto(
                    projection.getEventId(),
                    projection.getCount()
            );
        }

        return new ConfirmedRequestsCountDto(eventId, 0L);
    }

}
