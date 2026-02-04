package ru.yandex.practicum.kafka.deserializer;


import ru.yandex.practicum.stats.avro.EventSimilarityAvro;

public class EventsSimilarityAvroDeserializer extends BaseAvroDeserializer<EventSimilarityAvro> {
    public EventsSimilarityAvroDeserializer() {
        super(EventSimilarityAvro.getClassSchema());
    }
}
