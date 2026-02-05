package ru.yandex.practicum.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.yandex.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.yandex.practicum.model.EventSimilarity;

@Mapper(componentModel = "spring")
public interface EventSimilarityMapper {

    @Mapping(target = "id", ignore = true)
    EventSimilarity toEventSimilarity(EventSimilarityAvro avro);

}
