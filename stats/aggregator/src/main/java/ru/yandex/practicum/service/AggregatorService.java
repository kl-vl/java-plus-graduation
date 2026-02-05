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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class AggregatorService {

    private static final double DEFAULT_WEIGHT = 0.0;
    private static final double MAX_WEIGHT = 1.0;
    private static final double EPSILON = 0.00001;
    private static final int SCALE = 2;
    //private static final double CHANGE_THRESHOLD = 0.01;
    private static final Map<ActionTypeAvro, Double> WEIGHT_CACHE = Map.of(
            ActionTypeAvro.VIEW, 0.4,
            ActionTypeAvro.REGISTER, 0.8,
            ActionTypeAvro.LIKE, 1.0
    );

    // Map<Event, Map<User, Weight>> - максимальные веса пользователей
    private final Map<Long, Map<Long, Double>> eventUserWeights = new ConcurrentHashMap<>();
    // Map<Event, Double> - СУММА ВЕСОВ для расчета нормы √Σw
    private final Map<Long, Double> eventWeightSums = new ConcurrentHashMap<>();
    // Map[EventA, Map[EventB, Double]] - суммы минимальных весов для пар (eventA < eventB)
    private final Map<Long, Map<Long, Double>> eventPairMinSums = new ConcurrentHashMap<>();

    // Кэш последних отправленных значений сходства
    private final Map<Long, Map<Long, Double>> lastSentSimilarities = new ConcurrentHashMap<>();

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

        // Вес не увеличился - ничего не делаем (сохраняем только максимальный вес)
        if (currentWeight != null && newWeight <= currentWeight) {
            return Collections.emptyList();
        }

        userWeights.put(userId, newWeight);

        // Атомарно обновляем сумму весов мероприятия
        double weightDelta = (currentWeight == null) ? newWeight : (newWeight - currentWeight);
        eventWeightSums.compute(eventId, (k, v) -> {
            double currentSum = (v == null) ? DEFAULT_WEIGHT : v;
            return currentSum + weightDelta;
        });

        // Обновляем пары мероприятий ТОЛЬКО с теми, где пользователь уже взаимодействовал
        List<EventSimilarityAvro> updatedSimilarities = updateEventPairs(eventId, userId, newWeight, currentWeight);

//        // Сохраняем новый максимальный вес пользователя ПОСЛЕ расчета пар
//        userWeights.put(userId, newWeight);

        return updatedSimilarities;
    }

    private List<EventSimilarityAvro> updateEventPairs(Long updatedEventId, Long userId,
                                                       Double newWeight, Double oldWeight) {
        List<EventSimilarityAvro> updatedSimilarities = new ArrayList<>();

        Set<Long> userEventIds = new HashSet<>();
        for (Map.Entry<Long, Map<Long, Double>> entry : eventUserWeights.entrySet()) {
            Long eventId = entry.getKey();
            if (eventId.equals(updatedEventId)) {
                continue;
            }
            Map<Long, Double> weights = entry.getValue();
            if (weights.containsKey(userId)) {
                userEventIds.add(eventId);
            }
        }

        // только пары с мероприятиями, где пользователь уже есть
        for (Long otherEventId : userEventIds) {
            Map<Long, Double> otherUserWeights = eventUserWeights.get(otherEventId);
            Double otherWeight = otherUserWeights.get(userId); // guaranteed not null

            // определяем упорядоченную пару (меньший id первым)
            long eventA = Math.min(updatedEventId, otherEventId);
            long eventB = Math.max(updatedEventId, otherEventId);

            // рассчитываем изменение суммы минимумов
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
                        return null; // Удаляем запись если сумма близка к нулю
                    }
                    return newSum;
                });

                // Удаляем пустую внутреннюю карту
                if (innerMap.isEmpty()) {
                    return null;
                }
                return innerMap;
            });

            // пересчитываем схожесть при обновлении веса пользователя
            EventSimilarityAvro similarity = calculateSimilarity(eventA, eventB);
            if (similarity != null && shouldSendSimilarity(eventA, eventB, similarity.getScore())) {
                updatedSimilarities.add(similarity);
                updateLastSentSimilarity(eventA, eventB, similarity.getScore());
            }
        }

        return updatedSimilarities;
    }

    private boolean shouldSendSimilarity(long eventA, long eventB, double newSimilarity) {
        Double lastSent = getLastSentSimilarity(eventA, eventB);

        // Если раньше не отправляли - отправляем
        if (lastSent == null) {
            return true;
        }

        // Отправляем только если значение изменилось значительно (> 0.01)
        double diff = Math.abs(lastSent - newSimilarity);
        return diff > EPSILON;
    }

    private Double getLastSentSimilarity(long eventA, long eventB) {
        Map<Long, Double> innerMap = lastSentSimilarities.get(eventA);
        return (innerMap != null) ? innerMap.get(eventB) : null;
    }

    private void updateLastSentSimilarity(long eventA, long eventB, double similarity) {
        lastSentSimilarities
                .computeIfAbsent(eventA, k -> new ConcurrentHashMap<>())
                .put(eventB, similarity);
    }

    private EventSimilarityAvro calculateSimilarity(long eventA, long eventB) {
        Double sumA = eventWeightSums.get(eventA);
        Double sumB = eventWeightSums.get(eventB);

        // Проверяем, что у обоих мероприятий есть взаимодействия
        if (sumA == null || sumB == null || sumA <= EPSILON || sumB <= EPSILON) {
            return null;
        }

        // Получаем сумму минимальных весов для пары
        Double minSum = eventPairMinSums
                .getOrDefault(eventA, Collections.emptyMap())
                .get(eventB);

        if (minSum == null || minSum <= EPSILON) {
            return null;
        }

        // схожесть
        double normA = Math.sqrt(sumA);
        double normB = Math.sqrt(sumB);
        double similarity = minSum / (normA * normB);

        // Ограничиваем значение в диапазоне [0, 1] и округляем до 2 знаков
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