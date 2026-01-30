package ru.yandex.practicum.compilation;

import org.mapstruct.BeforeMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.yandex.practicum.dto.compilation.CompilationCreateDto;
import ru.yandex.practicum.dto.compilation.CompilationDto;
import ru.yandex.practicum.config.CommonMapperConfiguration;
import ru.yandex.practicum.event.EventMapper;

import java.util.HashSet;
import java.util.List;

@Mapper(uses = EventMapper.class, config = CommonMapperConfiguration.class)
public interface CompilationMapper {
    @BeforeMapping()
    default void validate(Compilation compilation) {
        if ((compilation != null) && (compilation.getEvents() == null)) {
            compilation.setEvents(new HashSet<>());
        }
    }

    @BeforeMapping()
    default void validate(CompilationCreateDto compilation) {
        if ((compilation != null) && (compilation.getEvents() == null)) {
            compilation.setEvents(new HashSet<>());
        }
        if ((compilation != null) && (compilation.getPinned() == null)) {
            compilation.setPinned(false);
        }
    }

    Compilation toEntity(CompilationDto compilationDto);

    @Mapping(target = "events", source = "events", ignore = true)
    Compilation toEntity(CompilationCreateDto compilationCreateDto);

    CompilationDto toDto(Compilation entity);

    List<CompilationDto> toDtoList(List<Compilation> compilations);


}
