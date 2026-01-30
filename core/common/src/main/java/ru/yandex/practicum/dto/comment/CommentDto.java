package ru.yandex.practicum.dto.comment;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Builder
@Setter
@Getter
public class CommentDto {
    private Long id;
    private String text;
    private CommentStatus status;
    private Long authorId;
    private LocalDateTime created;
    private Long eventId;

}
