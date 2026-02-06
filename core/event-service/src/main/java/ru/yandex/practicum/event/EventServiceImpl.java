package ru.yandex.practicum.event;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;
import ru.yandex.practicum.category.Category;
import ru.yandex.practicum.category.CategoryRepository;
import ru.yandex.practicum.client.AnalyzerClient;
import ru.yandex.practicum.client.CollectorClient;
import ru.yandex.practicum.client.service.RequestServiceClient;
import ru.yandex.practicum.client.service.UserServiceClient;
import ru.yandex.practicum.dto.BooleanResponseDto;
import ru.yandex.practicum.dto.event.EventDto;
import ru.yandex.practicum.dto.event.EventDtoFull;
import ru.yandex.practicum.dto.event.EventDtoShort;
import ru.yandex.practicum.dto.event.EventFilterAdmin;
import ru.yandex.practicum.dto.event.EventFilterBase;
import ru.yandex.practicum.dto.event.EventFilterPublic;
import ru.yandex.practicum.dto.event.enums.EventState;
import ru.yandex.practicum.dto.event.enums.EventStateAction;
import ru.yandex.practicum.dto.request.ConfirmedRequestsCount;
import ru.yandex.practicum.dto.request.ConfirmedRequestsCountDto;
import ru.yandex.practicum.dto.user.UserDto;
import ru.yandex.practicum.exception.CategoryNotFoundException;
import ru.yandex.practicum.exception.EventAlreadyPublishedException;
import ru.yandex.practicum.exception.EventCanceledCantPublishException;
import ru.yandex.practicum.exception.EventDateException;
import ru.yandex.practicum.exception.EventNotFoundException;
import ru.yandex.practicum.exception.FilterValidationException;
import ru.yandex.practicum.exception.ServiceException;
import ru.yandex.practicum.exception.UserNotFoundException;
import ru.yandex.practicum.grpc.similarity.reports.RecommendedEventProto;
import ru.yandex.practicum.grpc.user.action.ActionTypeProto;
import ru.yandex.practicum.location.Location;
import ru.yandex.practicum.location.LocationRepository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final EventMapper eventMapper;
    private final Validator validator;
    private final CategoryRepository categoryRepository;
    private final LocationRepository locationRepository;
    private final UserServiceClient userServiceClient;
    private final RequestServiceClient requestServiceClient;
    private final CollectorClient collectorClient;
    private final AnalyzerClient analyzerClient;

    private void validateFilter(EventFilterBase filter) throws FilterValidationException, EventDateException {

        validateDateRange(filter.getRangeStart(), filter.getRangeEnd());

        Errors errors = new BeanPropertyBindingResult(filter, "filter");
        ValidationUtils.invokeValidator(validator, filter, errors);

        if (errors.hasErrors()) {
            String errorMessage = errors.getAllErrors().stream()
                    .map(DefaultMessageSourceResolvable::getDefaultMessage)
                    .collect(Collectors.joining(", "));

            throw new FilterValidationException("Filter validation failed: " + errorMessage);
        }
    }

    private void validateDateRange(LocalDateTime rangeStart, LocalDateTime rangeEnd) throws EventDateException {
        if (rangeStart != null && rangeEnd != null && rangeStart.isAfter(rangeEnd)) {
            throw new EventDateException("Start date must be before end date");
        }
    }

    private void setDefaultValues(EventDto eventDto) {
        if (eventDto.getPaid() == null) {
            eventDto.setPaid(false);
        }
        if (eventDto.getRequestModeration() == null) {
            eventDto.setRequestModeration(true);
        }
        if (eventDto.getParticipantLimit() == null) {
            eventDto.setParticipantLimit(0);
        }
    }

    private List<EventDtoFull> getDtoFullList(Page<Event> pageEvents) {
        return pageEvents.stream()
                .map(eventMapper::toEventFullDto)
                .collect(Collectors.toList());
    }

    private void enrichEventsWithConfirmedRequests(List<EventDtoFull> events) {
        if (events.isEmpty()) {
            return;
        }
        List<Long> eventIds = events.stream()
                .map(EventDtoFull::getId)
                .collect(Collectors.toList());

        List<ConfirmedRequestsCountDto> results = requestServiceClient.getConfirmedRequestsList(eventIds);
        Map<Long, Long> confirmedRequestsMap = results.stream()
                .collect(Collectors.toMap(
                        ConfirmedRequestsCount::getEventId,
                        ConfirmedRequestsCount::getCount
                ));

        events.forEach(event -> {
            Long confirmedRequests = confirmedRequestsMap.getOrDefault(event.getId(), 0L);
            event.setConfirmedRequests(confirmedRequests);
        });
    }

    // Получение статистики по списку событий и обогащение views
    private void enrichEventsWithViews(List<EventDtoFull> events, EventFilterAdmin eventFilter) {
        if (events.isEmpty()) {
            return;
        }
        List<Long> eventIds = events.stream()
                .map(EventDtoFull::getId)
                .toList();

        List<String> uris = eventIds.stream()
                .map(id -> "/events/" + id)
                .collect(Collectors.toList());

        LocalDateTime start = eventFilter.getRangeStart() != null ? eventFilter.getRangeStart() : LocalDateTime.now().minusDays(1);
        LocalDateTime end = eventFilter.getRangeEnd() != null ? eventFilter.getRangeEnd() : LocalDateTime.now().plusDays(1);
    }

    // Admin
    @Override
    public List<EventDtoFull> findEventsByUsers(EventFilterAdmin eventFilter) throws FilterValidationException, EventDateException {
        validateFilter(eventFilter);

        log.info("Event-service. findEventsByUsers input: filter = {}", eventFilter);

        Specification<Event> specification = EventSpecifications.forAdminFilter(eventFilter);

        Pageable pageable = PageRequest.of(eventFilter.getFrom() / eventFilter.getSize(), eventFilter.getSize());

        Page<Event> pageEvents = eventRepository.findAll(specification, pageable);
        final List<EventDtoFull> events = getDtoFullList(pageEvents);

        enrichEventsWithConfirmedRequests(events);
        enrichEventsWithViews(events, eventFilter);

        log.info("Event-service. findEventsByUsers success: size = {}", events.size());

        return events;
    }

    @Override
    @Transactional
    public EventDtoFull updateEventById(EventDto eventDto) throws EventNotFoundException, EventDateException, EventAlreadyPublishedException, EventCanceledCantPublishException {
        log.info("Event-service. updateEventById input: id = {}", eventDto.getId());
        Event event = eventRepository.findById(eventDto.getId())
                .orElseThrow(() -> new EventNotFoundException("Event with id=%d was not found".formatted(eventDto.getId())));

        /*
         * Редактирование данных любого события администратором. Валидация данных не требуется. Обратите внимание:
         * дата начала изменяемого события должна быть не ранее чем за час от даты публикации. (Ожидается код ошибки 409)
         * событие можно публиковать, только если оно в состоянии ожидания публикации (Ожидается код ошибки 409)
         * событие можно отклонить, только если оно еще не опубликовано (Ожидается код ошибки 409)
         */

        if (eventDto.getEventDate() != null && !eventDto.getEventDate().isAfter(LocalDateTime.now().plusHours(1))) {
            throw new EventDateException("Event date should be in 1+ hours after now");
        }

        if (event.getState().equals(EventState.PUBLISHED)) {
            throw new EventAlreadyPublishedException("Event is already published");
        }

        if (event.getState().equals(EventState.CANCELED)) {
            throw new EventCanceledCantPublishException("Canceled event cant be published");
        }

        if (eventDto.getStateAction() != null) {
            if (eventDto.getStateAction().equals(EventStateAction.PUBLISH_EVENT)) {
                event.setState(EventState.PUBLISHED);
                event.setPublishedOn(LocalDateTime.now());
                // прикладной логикой смена флага не управляется, как и в тестах постмана не участвует
                //event.setRequestModeration(false);
            }
            if (eventDto.getStateAction().equals(EventStateAction.REJECT_EVENT)) {
                event.setState(EventState.CANCELED);
            }
        }

        eventMapper.updateEventFromDto(eventDto, event);
        Event updatedEvent = eventRepository.save(event);

        log.info("Event-service. updateEventById success: id = {}", updatedEvent.getId());

        return eventMapper.toEventFullDto(updatedEvent);
    }

    // Private
    @Override
    @Transactional
    public EventDtoFull createEvent(EventDto eventDto) throws
            CategoryNotFoundException, EventDateException, UserNotFoundException, ServiceException {
        log.info("Event-service. createEvent input: id = {}", eventDto.getDescription());

        setDefaultValues(eventDto);

        if (eventDto.getEventDate() != null && !eventDto.getEventDate().isAfter(LocalDateTime.now().plusHours(1))) {
            throw new EventDateException("Event date should be in 1+ hours after now");
        }

        Event event = eventMapper.toEvent(eventDto);
        if (eventDto.getCategory() != null) {
            Category category = categoryRepository.findById(eventDto.getCategory())
                    .orElseThrow(() -> new CategoryNotFoundException(String.format("Category with id=%d was not found", eventDto.getCategory())));
            event.setCategory(category);
        }


        if (eventDto.getInitiator() != null) {
            UserDto userDto = userServiceClient.fetchUserDto(eventDto.getInitiator());
            event.setInitiatorId(userDto.getId());
        }

        if (eventDto.getLocation() != null) {
            Location location = locationRepository.findByLatAndLon(
                    eventDto.getLocation().getLat(),
                    eventDto.getLocation().getLon()
            ).orElseGet(() -> {
                Location newLocation = new Location();
                newLocation.setLat(eventDto.getLocation().getLat());
                newLocation.setLon(eventDto.getLocation().getLon());
                return locationRepository.save(newLocation);
            });
            event.setLocation(location);
        }

        event.setState(EventState.PENDING);
        event.setCreatedOn(LocalDateTime.now());
        event.setConfirmedRequests(0L);

        Event createdEvent = eventRepository.save(event);

        log.info("Event-service. createEvent success: id = {}", createdEvent.getId());

        return eventMapper.toEventFullDto(createdEvent);
    }

    @Override
    public EventDtoFull findEventByUserId(Long userId, Long eventId) throws EventNotFoundException {
        log.info("Event-service. findEventByUserId input: userId = {}, eventId = {}", userId, eventId);

        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId).orElseThrow(() -> new EventNotFoundException("Event with id " + eventId + " not found"));

        log.info("Event-service. findEventByUserId success: id = {}", event.getId());

        return eventMapper.toEventFullDto(event);
    }

    @Override
    public List<EventDtoFull> findEventsByUserid(Long userId, int from, int size) {
        log.info("Event-service. findEventsByUserid input: userId = {}, from = {}, size = {}", userId, from, size);

        Pageable pageable = PageRequest.of(from / size, size);

        Page<Event> pageEvents = eventRepository.findAllByInitiatorId(userId, pageable);
        final List<EventDtoFull> events = getDtoFullList(pageEvents);

        log.info("Event-service. findEventsByUserid success: id = {}", events.size());

        return events;
    }

    @Override
    @Transactional
    public EventDtoFull updateEventByUserId(EventDto eventDto) throws EventNotFoundException, EventDateException, EventCanceledCantPublishException {
        // изменить можно только отмененные события или события в состоянии ожидания модерации (Ожидается код ошибки 409)
        //дата и время на которые намечено событие не может быть раньше, чем через два часа от текущего момента (Ожидается код ошибки 409)
        log.info("Event-service. updateEventByUserId input: eventId = {}, userId = {}", eventDto.getId(), eventDto.getInitiator());

        if (eventDto.getEventDate() != null && !eventDto.getEventDate().isAfter(LocalDateTime.now().plusHours(2))) {
            throw new EventDateException("Event date should be in 2+ hours after now");
        }

        Event existingEvent = eventRepository.findByIdAndInitiatorId(eventDto.getId(), eventDto.getInitiator())
                .orElseThrow(() -> new EventNotFoundException("Event with id " + eventDto.getId() + " not found"));

        if (!(existingEvent.getState() == EventState.PENDING || existingEvent.getState() == EventState.CANCELED)) {
            throw new EventCanceledCantPublishException("Event can be edited only Pending or Canceled");
        }

        eventMapper.updateEventFromDto(eventDto, existingEvent);

        if (eventDto.getStateAction() != null) {
            if (eventDto.getStateAction().equals(EventStateAction.SEND_TO_REVIEW)) {
                existingEvent.setState(EventState.PENDING);
            } else {
                existingEvent.setState(EventState.CANCELED);
            }
        }
        Event updatedEvent = eventRepository.save(existingEvent);

        log.info("Event-service. updateEventByUserId success: eventId = {}", updatedEvent.getId());

        return eventMapper.toEventFullDto(updatedEvent);
    }

    //Public
    @Override
    public List<EventDtoFull> findEvents(EventFilterPublic eventFilter, HttpServletRequest request) throws
            FilterValidationException, EventDateException {
        validateFilter(eventFilter);

        log.info("Event-service. findEventsByUsers input: filter = {}", eventFilter);

        Specification<Event> specification = EventSpecifications.forPublicFilter(eventFilter);
        Pageable pageable = PageRequest.of(eventFilter.getFrom() / eventFilter.getSize(), eventFilter.getSize());
        Page<Event> pageEvents = eventRepository.findAll(specification, pageable);
        final List<EventDtoFull> events = getDtoFullList(pageEvents);

        log.info("Event-service. findEventsByUsers success: size = {}", events.size());

        return events;
    }

    @Override
    public EventDtoFull findEventById(Long eventId, HttpServletRequest request) throws EventNotFoundException {
        log.info("Event-service. findEventById input: eventId = {}", eventId);

        Event event = eventRepository.findByIdAndState(eventId, EventState.PUBLISHED).orElseThrow(() -> new EventNotFoundException("Event with id " + eventId + " not found"));

        log.info("Event-service. findEventById success: eventId = {}", event.getId());

        EventDtoFull eventDto = eventMapper.toEventFullDto(event);

        enrichEventWithAdditionalData(eventDto);

        return eventDto;
    }

    @Override
    public EventDtoFull findEventById(Long eventId, Long userId) throws EventNotFoundException {
        log.info("Event-service. findEventById input: eventId = {}, userId = {}", eventId, userId);

        Event event = eventRepository.findByIdAndState(eventId, EventState.PUBLISHED).orElseThrow(() -> new EventNotFoundException("Event with id " + eventId + " not found"));

        // при обработке запроса к эндпоинту GET /events/{id} необходимо отправить информацию о просмотре пользователем мероприятия с идентификатором id
        collectorClient.collectUserAction(userId, eventId, ActionTypeProto.ACTION_VIEW.toString(), Instant.now());

        log.info("Event-service. findEventById success: eventId = {}", event.getId());

        return eventMapper.toEventFullDto(event);
    }


    @Override
    public List<EventDtoShort> getRecommendations(Long max, Long userId) {
        log.info("Event-service. getRecommendations input: max = {}, userId = {}", max, userId);

        Map<Long, Double> recommendations = analyzerClient.getRecommendationsForUser(userId, max)
                .collect(Collectors.toMap(RecommendedEventProto::getEventId, RecommendedEventProto::getScore));

        if (recommendations.isEmpty()) {
            log.info("Event-service. getRecommendations: No recommendations for userId={}", userId);
            return List.of();
        }

        List<EventDtoShort> result = eventRepository.findAllById(recommendations.keySet()).stream()
                .filter(event -> event.getState() == EventState.PUBLISHED)
                .map(event -> {
                    EventDtoShort dto = eventMapper.toEventDtoShort(event);
                    dto.setRating(recommendations.get(event.getId()));
                    return dto;
                })
                .sorted(Comparator.comparingDouble(EventDtoShort::getRating).reversed())
                .limit(max)
                .toList();

        log.info("Event-service. getRecommendations: {} recommendations for userId={}", result.size(), userId);
        return result;
    }

    @Override
    public void addLike(Long eventId, Long userId) throws EventNotFoundException, ServiceException, UserNotFoundException {
        log.info("Event-service. addLike input: eventId = {}, userId = {}", eventId, userId);

        Event event = eventRepository.findById(eventId).orElseThrow(() -> new EventNotFoundException(eventId));

        if (requestServiceClient.checkUserParticipation(userId, eventId)) {
            collectorClient.collectUserAction(userId, eventId, ActionTypeProto.ACTION_LIKE.toString(), Instant.now());
        } else {
            throw new UserNotFoundException("The user %s did not register for event %s".formatted(userId, eventId));
        }

    }

    private void enrichEventWithAdditionalData(EventDtoFull event) {
        Long confirmedRequests = requestServiceClient.getConfirmedRequestCount(event.getId());
        log.info("Event-service. enrichEvent: eventId = {}, confirmedRequests = {} ", event.getId(), confirmedRequests);
        event.setConfirmedRequests(confirmedRequests);

    }

    @Override
    public EventDtoFull findEventById(Long eventId) throws EventNotFoundException {
        log.info("Event-service. findEventById input: eventId = {}", eventId);

        Event event = eventRepository.findById(eventId).orElseThrow(() -> new EventNotFoundException("Event with id " + eventId + " not found"));

        log.info("Event-service. findEventById success: eventId = {}", event.getId());

        EventDtoFull eventDto = eventMapper.toEventFullDto(event);

        enrichEventWithAdditionalData(eventDto);

        return eventDto;
    }

    @Override
    public BooleanResponseDto existsEvent(Long eventId) {
        log.info("Event-service. existsEvent input: eventId = {}", eventId);

        boolean exists = eventRepository.existsById(eventId);

        log.info("{}. existsUser success: userId = {} {}", "Event-service", eventId, exists ? "exists" : "does not exist");

        return new BooleanResponseDto(exists);
    }
}