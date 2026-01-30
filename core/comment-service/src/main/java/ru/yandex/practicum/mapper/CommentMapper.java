package ru.yandex.practicum.mapper;

import org.mapstruct.Mapper;
import ru.yandex.practicum.config.CommonMapperConfiguration;
import ru.yandex.practicum.dto.comment.CommentDto;
import ru.yandex.practicum.dto.comment.CommentDtoShort;
import ru.yandex.practicum.model.Comment;

@Mapper(config = CommonMapperConfiguration.class)
public interface CommentMapper {
    Comment toEntity(CommentDto commentDto);

    CommentDto toDto(Comment entity);

    CommentDtoShort toDtoShort(Comment entity);
}
