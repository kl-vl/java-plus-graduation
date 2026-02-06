package ru.yandex.practicum.service;

import lombok.extern.slf4j.Slf4j;
import ru.yandex.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.yandex.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.yandex.practicum.ewm.stats.avro.UserActionAvro;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class AggregatorService {

    private static final double DEFAULT_WEIGHT = 0.0;
    private static final double MAX_WEIGHT = 1.0;
    private static final double EPSILON = 1e-6;
    private static final int SCALE = 2;
    private static final Map<ActionTypeAvro, Double> WEIGHT_CACHE = Map.of(
            ActionTypeAvro.VIEW, 0.4,
            ActionTypeAvro.REGISTER, 0.8,
            ActionTypeAvro.LIKE, 1.0
    );

    // Map<Event, Map<User, Weight>> - максимальные веса пользователей
    private final Map<Long, Map<Long, Double>> eventUserWeights = new ConcurrentHashMap<>();
    // Map<Event, Double> - сумма весов для расчета нормы
    private final Map<Long, Double> eventWeightSums = new ConcurrentHashMap<>();
    // Map[EventA, Map[EventB, Double]] - суммы минимальных весов для пар (eventA < eventB)
    private final Map<Long, Map<Long, Double>> eventPairMinSums = new ConcurrentHashMap<>();

    public List<EventSimilarityAvro> calculateSimilarity(UserActionAvro request) {
        double newWeight = getWeight(request.getActionType());
        return updateEventWeight(request.getEventId(), request.getUserId(), newWeight);
    }

    private double getWeight(ActionTypeAvro actionType) {
        return WEIGHT_CACHE.getOrDefault(actionType, DEFAULT_WEIGHT);
    }

    private List<EventSimilarityAvro> updateEventWeight(Long eventId, Long userId, Double newWeight) {
        Map<Long, Double> userWeights = eventUserWeights.computeIfAbsent(eventId, k -> new ConcurrentHashMap<>());
        Double currentWeight = userWeights.get(userId);

        // вес не увеличился - ничего не делаем (сохраняем только максимальный вес)
        if (currentWeight != null && newWeight <= currentWeight) {
            return Collections.emptyList();
        }

        // сохраняем вес пользователя
        userWeights.put(userId, newWeight);

        // обновляем сумму весов мероприятия
        double weightDelta = (currentWeight == null) ? newWeight : (newWeight - currentWeight);
        eventWeightSums.compute(eventId, (k, v) -> {
            double currentSum = (v == null) ? DEFAULT_WEIGHT : v;
            return currentSum + weightDelta;
        });

        // обновляем пары мероприятий только с теми, где пользователь уже взаимодействовал
        return updateEventPairs(eventId, userId, newWeight, currentWeight);
    }

    private List<EventSimilarityAvro> updateEventPairs(Long updatedEventId, Long userId,
                                                       Double newWeight, Double oldWeight) {
        List<EventSimilarityAvro> updatedSimilarities = new ArrayList<>();

        // ищем другие мероприятия, с которыми взаимодействовал этот пользователь
        for (Map.Entry<Long, Map<Long, Double>> entry : eventUserWeights.entrySet()) {
            Long otherEventId = entry.getKey();
            if (updatedEventId.equals(otherEventId)) {
                continue;
            }

            Map<Long, Double> otherUserWeights = entry.getValue();
            Double otherWeight = otherUserWeights.get(userId);
            if (otherWeight == null) {
                continue;
            }

            // определяем упорядоченную пару мероприятий (меньший ID первым)
            long eventA = Math.min(updatedEventId, otherEventId);
            long eventB = Math.max(updatedEventId, otherEventId);

            double oldMin = (oldWeight != null) ? Math.min(oldWeight, otherWeight) : DEFAULT_WEIGHT;
            double newMin = Math.min(newWeight, otherWeight);
            double minDelta = newMin - oldMin;

            // обновляем сумму минимальных весов для пары
            eventPairMinSums.compute(eventA, (k, innerMap) -> {
                if (innerMap == null) {
                    innerMap = new ConcurrentHashMap<>();
                }
                innerMap.compute(eventB, (key, currentValue) -> {
                    double currentSum = (currentValue == null) ? DEFAULT_WEIGHT : currentValue;
                    double newSum = currentSum + minDelta;

                    if (newSum <= EPSILON) {
                        return null;
                    }
                    return newSum;
                });

                if (innerMap.isEmpty()) {
                    return null;
                }
                return innerMap;
            });

            EventSimilarityAvro similarity = calculateSimilarity(eventA, eventB);
            if (similarity != null) {
                updatedSimilarities.add(similarity);
            }
        }

        return updatedSimilarities;
    }

    private EventSimilarityAvro calculateSimilarity(long eventA, long eventB) {
        Double sumA = eventWeightSums.get(eventA);
        Double sumB = eventWeightSums.get(eventB);

        // у обоих мероприятий есть взаимодействия
        if (sumA == null || sumB == null || sumA <= EPSILON || sumB <= EPSILON) {
            return null;
        }

        // сумма минимальных весов для пары
        Double minSum = eventPairMinSums
                .getOrDefault(eventA, Collections.emptyMap())
                .get(eventB);

        if (minSum == null || minSum <= EPSILON) {
            return null;
        }

        // формула схожести
        double normA = Math.sqrt(sumA);
        double normB = Math.sqrt(sumB);
        double similarity = minSum / (normA * normB);

        // ограничение значения в диапазоне [0, 1]
        similarity = Math.max(DEFAULT_WEIGHT, Math.min(MAX_WEIGHT, similarity));
        similarity = round(similarity);

        return new EventSimilarityAvro(eventA, eventB, similarity, Instant.now());
    }

    private double round(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return DEFAULT_WEIGHT;
        }
        return BigDecimal.valueOf(value)
                .setScale(SCALE, RoundingMode.HALF_UP)
                .doubleValue();
    }
}