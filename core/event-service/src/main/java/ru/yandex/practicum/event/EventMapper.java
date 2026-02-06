package ru.yandex.practicum.event;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import ru.yandex.practicum.category.Category;
import ru.yandex.practicum.dto.category.CategoryDto;
import ru.yandex.practicum.config.CommonMapperConfiguration;
import ru.yandex.practicum.dto.event.EventDto;
import ru.yandex.practicum.dto.event.EventDtoFull;
import ru.yandex.practicum.dto.event.EventDtoShort;
import ru.yandex.practicum.location.Location;
import ru.yandex.practicum.dto.location.LocationDto;

@Mapper(config = CommonMapperConfiguration.class)
public interface EventMapper {

    // EventDto только ID
    @Mapping(target = "category", source = "category", qualifiedByName = "mapCategoryToId")
    @Mapping(target = "initiator", source = "initiatorId")
    @Mapping(target = "location", source = "location", qualifiedByName = "mapLocationToDto")
    @Mapping(target = "rating", ignore = true)
    EventDto toEventDto(Event event);

    @Mapping(target = "category", source = "category", qualifiedByName = "mapCategoryToDto")
    @Mapping(target = "initiator", source = "initiatorId")
    EventDtoShort toEventDtoShort(Event event);

    // EventDtoFull - DTO
    @Mapping(target = "category", source = "category", qualifiedByName = "mapCategoryToDto")
    @Mapping(target = "initiator", source = "initiatorId")
    @Mapping(target = "location", source = "location", qualifiedByName = "mapLocationToDto")
    EventDtoFull toEventFullDto(Event event);

    @Mapping(target = "category", source = "category", qualifiedByName = "mapIdToCategory")
    @Mapping(target = "location", source = "location", qualifiedByName = "mapDtoToLocation")
    @Mapping(target = "state", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "publishedOn", ignore = true)
    @Mapping(target = "confirmedRequests", ignore = true)
    Event toEvent(EventDto eventDto);

    @Mapping(target = "category", ignore = true)
    @Mapping(target = "location", ignore = true)
    @Mapping(target = "state", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "publishedOn", ignore = true)
    @Mapping(target = "confirmedRequests", ignore = true)
    void updateEventFromDto(EventDto eventDto, @MappingTarget Event event);

    @Named("mapCategoryToId")
    default Long mapCategoryToId(Category category) {
        return category != null ? category.getId() : null;
    }

    @Named("mapCategoryToDto")
    default CategoryDto mapCategoryToDto(Category category) {
        if (category == null) {
            return null;
        }
        return CategoryDto.builder()
                .id(category.getId())
                .name(category.getName())
                .build();
    }

    @Named("mapLocationToDto")
    default LocationDto mapLocationToDto(Location location) {
        if (location == null) {
            return null;
        }
        return LocationDto.builder()
                .lat(location.getLat())
                .lon(location.getLon())
                .build();
    }

    @Named("mapIdToCategory")
    default Category mapIdToCategory(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        return Category.builder().id(categoryId).build();
    }

    @Named("mapDtoToLocation")
    default Location mapDtoToLocation(LocationDto locationDto) {
        if (locationDto == null) {
            return null;
        }
        return Location.builder()
                .lat(locationDto.getLat())
                .lon(locationDto.getLon())
                .build();
    }

}