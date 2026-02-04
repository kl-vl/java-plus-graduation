package ru.yandex.practicum.kafka.deserializer;

import ru.yandex.practicum.stats.avro.UserActionAvro;

public class UserActionDeserializer extends BaseAvroDeserializer<UserActionAvro> {
    public UserActionDeserializer() {
        super(UserActionAvro.getClassSchema());
    }
}
