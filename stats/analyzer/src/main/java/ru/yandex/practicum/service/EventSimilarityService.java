package ru.yandex.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.yandex.practicum.mapper.EventSimilarityMapper;
import ru.yandex.practicum.model.EventSimilarity;
import ru.yandex.practicum.repository.SimilarityRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class EventSimilarityService {
    private final SimilarityRepository repository;
    private final EventSimilarityMapper mapper;

    @Transactional
    public void saveSimilarity(EventSimilarityAvro request) {
        log.info("EventSimilarityService. Save similarity: eventA={}, eventB={}, score={}",
                request.getEventA(), request.getEventB(), request.getScore());
        repository.save(mapper.toEventSimilarity(request));
    }

    public List<EventSimilarity> getSimilarToEvent(Long eventId) {
        log.info("EventSimilarityService. Getting similarities for eventId={}", eventId);

        // Получаем все связи где eventId является eventA
        List<EventSimilarity> asEventA = repository.findByEventA(eventId);

        // Получаем все связи где eventId является eventB и меняем местами
        List<EventSimilarity> asEventB = repository.findByEventB(eventId).stream()
                .map(this::swapEvents)
                .collect(Collectors.toList());

        // Объединяем результаты
        List<EventSimilarity> result = new ArrayList<>(asEventA);
        result.addAll(asEventB);

        return result;
    }

    public List<EventSimilarity> getSimilarToEvents(List<Long> eventIds) {
        if (eventIds.isEmpty()) {
            return Collections.emptyList();
        }

        log.info("EventSimilarityService. Getting similarities for {} eventIds", eventIds.size());

        // Получаем все связи где eventIds содержатся в eventA
        List<EventSimilarity> asEventA = repository.findByEventAIn(eventIds);

        // Получаем все связи где eventIds содержатся в eventB и меняем местами
        List<EventSimilarity> asEventB = repository.findByEventBIn(eventIds).stream()
                .map(this::swapEvents)
                .collect(Collectors.toList());

        // Объединяем результаты
        List<EventSimilarity> result = new ArrayList<>(asEventA);
        result.addAll(asEventB);

        return result;
    }

    private EventSimilarity swapEvents(EventSimilarity similarity) {
        // Создаем новый объект с поменянными местами событиями
        return EventSimilarity.builder()
                .id(similarity.getId())
                .eventA(similarity.getEventB())
                .eventB(similarity.getEventA())
                .score(similarity.getScore())
                .timestamp(similarity.getTimestamp())
                .build();
    }

    // Опционально: метод для проверки существования связей
    public boolean hasSimilarities(Long eventId) {
        long count = repository.countByEventA(eventId) + repository.countByEventB(eventId);
        return count > 0;
    }
}
