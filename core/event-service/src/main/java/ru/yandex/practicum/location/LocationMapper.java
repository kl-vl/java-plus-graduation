package ru.yandex.practicum.location;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.yandex.practicum.config.CommonMapperConfiguration;
import ru.yandex.practicum.dto.location.LocationDto;

@Mapper(config = CommonMapperConfiguration.class)
public interface LocationMapper {
    @Mapping(target = "id", ignore = true)
    LocationDto toLocationDto(Location location);

    Location toLocation(LocationDto locationDto);
}
