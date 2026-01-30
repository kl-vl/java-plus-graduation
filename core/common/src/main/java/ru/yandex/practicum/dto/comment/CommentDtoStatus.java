package ru.yandex.practicum.dto.comment;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Setter
@Getter
public class CommentDtoStatus {
    @Null
    private Long id;

    @NotNull
    private CommentStatus status;

}
