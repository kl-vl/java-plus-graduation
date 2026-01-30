package ru.yandex.practicum.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.client.service.EventServiceClient;
import ru.yandex.practicum.client.service.UserServiceClient;
import ru.yandex.practicum.dto.comment.CommentDto;
import ru.yandex.practicum.dto.comment.CommentDtoReq;
import ru.yandex.practicum.dto.comment.CommentDtoShort;
import ru.yandex.practicum.dto.comment.CommentDtoStatus;
import ru.yandex.practicum.dto.comment.CommentStatus;
import ru.yandex.practicum.dto.event.EventDtoFull;
import ru.yandex.practicum.dto.user.UserDto;
import ru.yandex.practicum.exception.CommentNotFoundException;
import ru.yandex.practicum.exception.EventNotFoundException;
import ru.yandex.practicum.exception.ServiceException;
import ru.yandex.practicum.exception.UserNotFoundException;
import ru.yandex.practicum.mapper.CommentMapper;
import ru.yandex.practicum.model.Comment;
import ru.yandex.practicum.repository.CommentRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final CommentMapper commentMapper;
    private final UserServiceClient userServiceClient;
    private final EventServiceClient eventServiceClient;

    @Getter
    @Value("${spring.application.name}")
    private String serviceName;

    @Override
    public CommentDto findComment(Long commentId) throws CommentNotFoundException {
        log.info("{}. findComment id = {}", getServiceName(), commentId);
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new CommentNotFoundException("Комментарий не найден с ID %d".formatted(commentId)));
        log.info("{}. findComment success id = {}", getServiceName(), comment.getId());
        return commentMapper.toDto(comment);
    }

    @Override
    public CommentDto findComment(Long eventId, Long commentId) throws CommentNotFoundException {
        log.info("{}. findComment eventId = {}, commentId {}", getServiceName(), eventId, commentId);
        Comment comment = commentRepository.findByIdAndEventId(commentId, eventId)
                .orElseThrow(() -> new CommentNotFoundException(
                        "Комментарий не найден с ID %d для события с ID %d ".formatted(commentId, eventId)));
        log.info("{}. findComment success eventId = {}, commentId = {}", getServiceName(), comment.getEventId(), comment.getId());
        return commentMapper.toDto(comment);
    }

    @Override
    public List<CommentDtoShort> findEventComments(Long eventId, Pageable pageable) throws EventNotFoundException, ServiceException {
        log.info("{}. findEventComments eventId = {}, c пагинацией = {}", getServiceName(), eventId, pageable);

        eventServiceClient.validateEventExists(eventId);

        Page<Comment> comments = commentRepository.findByEventIdAndStatus(eventId, CommentStatus.PUBLISHED, pageable);

        log.info("{}. findEventComments success = {}", getServiceName(), comments.getSize());

        List<Long> authorIds = comments.getContent().stream()
                .map(Comment::getAuthorId)
                .collect(Collectors.toList());

        List<UserDto> authors = userServiceClient.fetchUsers(authorIds);

        Map<Long, String> authorNames = authors.stream()
                .collect(Collectors.toMap(UserDto::getId, UserDto::getName));

        return comments.stream()
                .map(comment -> {
                    CommentDtoShort dto = commentMapper.toDtoShort(comment);
                    dto.setAuthorName(authorNames.getOrDefault(comment.getAuthorId(), "Unknown"));
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<CommentDto> findUserComments(Long userId, Pageable pageable) throws UserNotFoundException, ServiceException {
        log.info("{}. findUserComments userId = {}", getServiceName(), userId);

        userServiceClient.validateUserExists(userId);

        Page<Comment> comments = commentRepository.findByAuthorId(userId, pageable);

        log.info("{}. findUserComments success = {}", getServiceName(), comments.getSize());

        return comments.stream()
                .map(commentMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CommentDto createComment(Long userId, Long eventId, CommentDto commentDto) throws EventNotFoundException, UserNotFoundException, ServiceException {
        log.info("{}. createComment userId = {}, eventId = {}", getServiceName(), userId, eventId);

        UserDto author = userServiceClient.fetchUserDto(userId);
        EventDtoFull eventDtoFull = eventServiceClient.fetchEventDto(eventId);

        Comment comment = commentMapper.toEntity(commentDto);
        comment.setAuthorId(author.getId());
        comment.setEventId(eventDtoFull.getId());
        comment.setCreated(LocalDateTime.now());
        comment.setStatus(CommentStatus.PENDING);

        Comment savedComment = commentRepository.save(comment);
        log.info("{}. createComment success id = {}", getServiceName(), savedComment.getId());
        return commentMapper.toDto(savedComment);
    }

    @Override
    @Transactional
    public CommentDto updateComment(CommentDtoStatus commentDto) throws CommentNotFoundException {
        log.info("{}. updateComment commentId = {} ", getServiceName(), commentDto.getId());
        Comment comment = commentRepository.findById(commentDto.getId())
                .orElseThrow(() -> new CommentNotFoundException(("Комментарий не найден с ID %d".formatted(commentDto.getId()))));
        if (commentDto.getStatus() != null) {
            comment.setStatus(commentDto.getStatus());
        }
        Comment updatedComment = commentRepository.save(comment);
        log.info("{}. updateComment success id = {} ", getServiceName(), updatedComment.getId());
        return commentMapper.toDto(updatedComment);
    }

    @Override
    @Transactional
    public CommentDto updateComment(Long userId, Long eventId, CommentDtoReq commentDto) throws CommentNotFoundException {
        log.info("{}. updateComment userId = {}, eventId = {}, commentId = {} ", getServiceName(), userId, eventId, commentDto.getId());
        Comment comment = commentRepository.findByIdAndEventIdAndAuthorId(commentDto.getId(), eventId, userId)
                .orElseThrow(() -> new CommentNotFoundException(
                        "Комментарий c ID %d не найден для события с ID %d или у вас нет прав для редактирования"
                                .formatted(commentDto.getId(), eventId)));

        // При редактировании комментария пользователем возвращаем комментарий на модерацию
        Optional.ofNullable(commentDto.getText()).ifPresent(text -> {
            comment.setText(text);
            comment.setStatus(CommentStatus.PENDING);
        });

        Comment updatedComment = commentRepository.save(comment);
        log.info("{}. updateComment success id = {} ", getServiceName(), updatedComment.getId());
        return commentMapper.toDto(updatedComment);
    }

    @Override
    @Transactional
    public void deleteComment(Long commentId) throws CommentNotFoundException {
        log.info("{}. deleteComment commentId = {} ", getServiceName(), commentId);
        if (!commentRepository.existsById(commentId)) {
            throw new CommentNotFoundException("Комментарий не найден с ID %d".formatted(commentId));
        }
        commentRepository.deleteById(commentId);
        log.info("{}. deleteComment success commentId = {} ", getServiceName(), commentId);
    }

    @Override
    @Transactional
    public void deleteComment(Long eventId, Long commentId) throws CommentNotFoundException {
        log.info("{}. deleteComment eventId = {}, commentId = {} ", getServiceName(), eventId, commentId);
        if (!commentRepository.existsByIdAndEventId(commentId, eventId)) {
            throw new CommentNotFoundException("Комментарий c ID %d не найден для события c ID %d".formatted(commentId, eventId));
        }
        commentRepository.deleteByIdAndEventId(commentId, eventId);
        log.info("{}. deleteComment success commentId = {} ", getServiceName(), commentId);
    }

    @Override
    @Transactional
    public void deleteComment(Long userId, Long eventId, Long commentId) throws CommentNotFoundException {
        log.info("{}. deleteComment userId = {}, eventId = {}, commentId = {} ", getServiceName(), userId, eventId, commentId);

        if (!commentRepository.existsByIdAndEventIdAndAuthorId(commentId, eventId, userId)) {
            throw new CommentNotFoundException(
                    "Комментарий c ID %d не найден для события с ID %d или у вас нет прав для для удаления"
                            .formatted(commentId, eventId));
        }
        commentRepository.deleteByIdAndEventIdAndAuthorId(commentId, eventId, userId);
        log.info("{}. deleteComment success id = {}", getServiceName(), commentId);
    }

}
