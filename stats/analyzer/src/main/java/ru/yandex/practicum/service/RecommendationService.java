package ru.yandex.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.grpc.similarity.reports.InteractionsCountRequestProto;
import ru.yandex.practicum.grpc.similarity.reports.RecommendedEventProto;
import ru.yandex.practicum.grpc.similarity.reports.SimilarEventsRequestProto;
import ru.yandex.practicum.grpc.similarity.reports.UserPredictionsRequestProto;
import ru.yandex.practicum.model.EventSimilarity;
import ru.yandex.practicum.model.UserAction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationService {

    private final EventSimilarityService similarityService;
    private final UserActionService userActionService;

    private static final int TOP_NEIGHBOURS = 10;

    public List<RecommendedEventProto> getRecommendationsForUser(UserPredictionsRequestProto request) {
        log.info("Получение рекомендаций для пользователя {}", request.getUserId());

        // 1. Получаем просмотренные события пользователя
        Map<Long, Double> seenEvents = getUserSeenEvents(request.getUserId());
        if (seenEvents.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. Ищем похожие события
        Map<Long, Double> similarEvents = findSimilarEvents(seenEvents.keySet());

        // 3. Фильтруем и сортируем кандидатов
        List<Long> candidateEvents = filterAndSortEvents(seenEvents, similarEvents, (long) request.getMaxResults());

        // 4. Рассчитываем финальные оценки
        Map<Long, Double> scoredEvents = calculateScores(candidateEvents, seenEvents);

        // 5. Строим ответ
        return buildRecommendations(scoredEvents);
    }

    public List<RecommendedEventProto> getSimilarEvents(SimilarEventsRequestProto request) {
        log.info("Поиск похожих событий для event {} пользователя {}",
                request.getEventId(), request.getUserId());

        Set<Long> seenEvents = getUserSeenEventIds(request.getUserId());

        return similarityService.getSimilarToEvent(request.getEventId()).stream()
                .filter(sim -> !seenEvents.contains(sim.getEventB()))
                .sorted(Comparator.comparing(EventSimilarity::getScore).reversed())
                .limit(request.getMaxResults())
                .map(sim -> buildRecommendation(sim.getEventB(), sim.getScore()))
                .collect(Collectors.toList());
    }

    public List<RecommendedEventProto> getInteractionsCount(InteractionsCountRequestProto request) {
        log.info("Расчет веса взаимодействий для {} событий", request.getEventIdCount());

        // Оптимизация: один запрос для всех eventId
        List<UserAction> userActions = userActionService.getMaxWeighted(request.getEventIdList());

        // Группируем по eventId и суммируем веса
        Map<Long, Double> eventWeights = userActions.stream()
                .collect(Collectors.toMap(
                        UserAction::getEventId,
                        UserAction::getActionWeight,
                        Double::sum
                ));

        return eventWeights.entrySet().stream()
                .map(entry -> buildRecommendation(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    private Map<Long, Double> getUserSeenEvents(Long userId) {
        return userActionService.getByUser(userId).stream()
                .collect(Collectors.toMap(
                        UserAction::getEventId,
                        UserAction::getActionWeight,
                        Math::max
                ));
    }

    private Set<Long> getUserSeenEventIds(Long userId) {
        return userActionService.getByUser(userId).stream()
                .map(UserAction::getEventId)
                .collect(Collectors.toSet());
    }

    private Map<Long, Double> findSimilarEvents(Set<Long> eventIds) {
        return similarityService.getSimilarToEvents(new ArrayList<>(eventIds)).stream()
                .collect(Collectors.toMap(
                        EventSimilarity::getEventB,
                        EventSimilarity::getScore,
                        Math::max
                ));
    }

    private List<Long> filterAndSortEvents(Map<Long, Double> seenEvents,
                                           Map<Long, Double> similarEvents,
                                           Long maxResults) {
        return similarEvents.entrySet().stream()
                .filter(entry -> !seenEvents.containsKey(entry.getKey()))
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(maxResults)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    private Map<Long, Double> calculateScores(List<Long> candidateEvents, Map<Long, Double> seenEvents) {
        Map<Long, Double> scoredEvents = new HashMap<>();

        for (Long candidateEvent : candidateEvents) {
            double score = calculateEventScore(candidateEvent, seenEvents);
            if (score > 0) {
                scoredEvents.put(candidateEvent, score);
            }
        }

        return scoredEvents;
    }

    private double calculateEventScore(Long eventId, Map<Long, Double> seenEvents) {
        // Получаем похожие события для кандидата
        List<EventSimilarity> neighbours = similarityService.getSimilarToEvent(eventId);

        // Фильтруем только те соседи, которые есть в просмотренных
        List<EventSimilarity> relevantNeighbours = neighbours.stream()
                .filter(neighbour -> seenEvents.containsKey(neighbour.getEventB()))
                .sorted(Comparator.comparing(EventSimilarity::getScore).reversed())
                .limit(TOP_NEIGHBOURS)
                .toList();

        if (relevantNeighbours.isEmpty()) {
            return 0;
        }

        // Рассчитываем взвешенную сумму
        double weightedSum = 0.0;
        double scoreSum = 0.0;

        for (EventSimilarity neighbour : relevantNeighbours) {
            double similarityScore = neighbour.getScore();
            double userWeight = seenEvents.get(neighbour.getEventB());

            weightedSum += similarityScore * userWeight;
            scoreSum += similarityScore;
        }

        return weightedSum / scoreSum;
    }

    private List<RecommendedEventProto> buildRecommendations(Map<Long, Double> scoredEvents) {
        return scoredEvents.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .map(entry -> buildRecommendation(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    private RecommendedEventProto buildRecommendation(Long eventId, Double score) {
        return RecommendedEventProto.newBuilder()
                .setEventId(eventId)
                .setScore(score)
                .build();
    }
}