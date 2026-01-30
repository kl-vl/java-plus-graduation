package ru.yandex.practicum.request.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.yandex.practicum.config.CommonMapperConfiguration;
import ru.yandex.practicum.dto.request.ParticipationRequestDto;
import ru.yandex.practicum.dto.request.RequestDto;
import ru.yandex.practicum.request.model.Request;

import java.util.List;
import java.util.stream.Collectors;

@Mapper(config = CommonMapperConfiguration.class)
public interface RequestMapper {

    @Mapping(target = "event", source = "eventId")
    @Mapping(target = "requester", source = "requesterId")
    RequestDto toDto(Request request);

    @Mapping(target = "created", ignore = true)
    @Mapping(target = "status", ignore = true)
    Request toEntity(RequestDto requestDto);

    List<RequestDto> toDtoList(List<Request> requests);

    @Mapping(target = "event", source = "eventId")
    @Mapping(target = "requester", source = "requesterId")
    ParticipationRequestDto toParticipationDto(Request request);

    default List<ParticipationRequestDto> toParticipationDtoList(List<Request> requests) {
        return requests.stream()
                .map(this::toParticipationDto)
                .collect(Collectors.toList());
    }

}