package ru.yandex.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.ewm.stats.avro.UserActionAvro;
import ru.yandex.practicum.mapper.UserActionMapper;
import ru.yandex.practicum.model.UserAction;
import ru.yandex.practicum.repository.UserActionRepository;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class UserActionService {
    private final UserActionMapper mapper;
    private final UserActionRepository repository;

    @Transactional
    public void saveUserAction(UserActionAvro request) {
        UserAction entity = mapper.toEntity(request);
        repository.save(entity);
        log.debug("Saved user action: userId={}, eventId={}",
                entity.getUserId(), entity.getEventId());
    }

    public List<UserAction> getMaxWeighted(List<Long> eventIds) {
        if (eventIds.isEmpty()) {
            return Collections.emptyList();
        }

        log.debug("Getting max weighted actions for {} event IDs", eventIds.size());
        return repository.findByEventIdIn(eventIds).stream()
                .collect(Collectors.collectingAndThen(
                        Collectors.groupingBy(
                                action -> action.getEventId() + "_" + action.getUserId(),
                                Collectors.maxBy(Comparator
                                        .comparing(UserAction::getActionWeight)
                                        .thenComparing(UserAction::getTimestamp))
                        ),
                        map -> map.values().stream()
                                .filter(Optional::isPresent)
                                .map(Optional::get)
                                .collect(Collectors.toList())
                ));
    }

    public List<UserAction> getByUser(Long userId) {
        log.debug("Getting user actions for userId={}", userId);
        return repository.findAllByUserId(userId);
    }
}