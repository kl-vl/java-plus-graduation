package ru.yandex.practicum.dto.comment;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Builder
@Setter
@Getter
public class CommentDtoShort {
    private Long id;
    private String text;
    private String authorName;
    private LocalDateTime created;
    private String eventIAnnotation;

}
