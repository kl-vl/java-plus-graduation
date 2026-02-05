package ru.yandex.practicum.model;

import ru.yandex.practicum.ewm.stats.avro.ActionTypeAvro;

public enum ActionType {
    VIEW("VIEW", 0.4),
    REGISTER("REGISTER", 0.8),
    LIKE("LIKE", 1.0);

    private final String avroName;
    private final double weight;

    ActionType(String avroName, double weight) {
        this.avroName = avroName;
        this.weight = weight;
    }

    public static ActionType fromAvro(ActionTypeAvro avroType) {
        if (avroType == null) {
            return null;
        }
        return switch (avroType) {
            case VIEW -> VIEW;
            case REGISTER -> REGISTER;
            case LIKE -> LIKE;
        };
    }

    public static double getWeightOrDefault(ActionType type, double defaultValue) {
        return type != null ? type.weight : defaultValue;
    }
}
