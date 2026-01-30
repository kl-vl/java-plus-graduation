package ru.yandex.practicum.service;

import org.springframework.data.domain.Pageable;
import ru.yandex.practicum.dto.comment.CommentDto;
import ru.yandex.practicum.dto.comment.CommentDtoReq;
import ru.yandex.practicum.dto.comment.CommentDtoShort;
import ru.yandex.practicum.dto.comment.CommentDtoStatus;
import ru.yandex.practicum.exception.CommentNotFoundException;
import ru.yandex.practicum.exception.EventNotFoundException;
import ru.yandex.practicum.exception.ServiceException;
import ru.yandex.practicum.exception.UserNotFoundException;

import java.util.List;

public interface CommentService {

    CommentDto createComment(Long userId, Long eventId, CommentDto commentDto) throws UserNotFoundException, EventNotFoundException, ServiceException;

    void deleteComment(Long userId, Long eventId, Long commentId) throws CommentNotFoundException;

    void deleteComment(Long eventId, Long commentId) throws CommentNotFoundException;

    void deleteComment(Long commentId) throws CommentNotFoundException;

    List<CommentDtoShort> findEventComments(Long eventId, Pageable pageable) throws EventNotFoundException, ServiceException;

    List<CommentDto> findUserComments(Long userId, Pageable pageable) throws UserNotFoundException, ServiceException;

    CommentDto findComment(Long commentId) throws CommentNotFoundException;

    CommentDto findComment(Long eventId, Long commentId) throws CommentNotFoundException;

    CommentDto updateComment(CommentDtoStatus commentDto) throws CommentNotFoundException;

    CommentDto updateComment(Long userId, Long eventId, CommentDtoReq commentDto) throws CommentNotFoundException;

}
