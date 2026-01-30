package ru.yandex.practicum.compilation.controller;

import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.compilation.CompilationService;
import ru.yandex.practicum.dto.compilation.CompilationCreateDto;
import ru.yandex.practicum.dto.compilation.CompilationDto;
import ru.yandex.practicum.exception.CompilationNotFoundException;
import ru.yandex.practicum.dto.validation.ValidationGroups;


@RestController
@RequestMapping(path = "/admin/compilations")
@RequiredArgsConstructor
@Validated
public class CompilationControllerAdmin {
    private final CompilationService compilationService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CompilationDto create(@RequestBody @Validated({ValidationGroups.Create.class}) CompilationCreateDto compilationDto) {
        return compilationService.create(compilationDto);
    }

    @DeleteMapping(path = "/{compilationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable @Positive Long compilationId) throws CompilationNotFoundException {
        compilationService.delete(compilationId);
    }

    @PatchMapping(path = "/{compilationId}")
    public CompilationDto update(@RequestBody @Validated({ValidationGroups.Update.class}) CompilationCreateDto compilationCreateDto,
                                 @PathVariable @Positive Long compilationId) throws CompilationNotFoundException {
        return compilationService.update(compilationId, compilationCreateDto);
    }
}
