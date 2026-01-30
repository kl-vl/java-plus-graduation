package ru.yandex.practicum.dto.compilation;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Builder
@ToString
@Getter
public class CompilationRequestParams {
    private Boolean pinned;
    private Integer from;
    private Integer size;
}
