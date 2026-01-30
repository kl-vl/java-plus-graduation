package ru.yandex.practicum.compilation;

import ru.yandex.practicum.dto.compilation.CompilationCreateDto;
import ru.yandex.practicum.dto.compilation.CompilationDto;
import ru.yandex.practicum.dto.compilation.CompilationRequestParams;
import ru.yandex.practicum.exception.CompilationNotFoundException;

import java.util.Collection;

public interface CompilationService {

    CompilationDto create(CompilationCreateDto compilationDto);

    CompilationDto getById(Long compilationId) throws CompilationNotFoundException;

    void delete(Long compilationId) throws CompilationNotFoundException;

    Collection<CompilationDto> findAll(CompilationRequestParams params);

    CompilationDto update(Long compilationId, CompilationCreateDto compilationCreateDto) throws CompilationNotFoundException;
}
