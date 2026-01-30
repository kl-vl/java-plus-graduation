package ru.yandex.practicum.controller;


import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.dto.comment.CommentDtoShort;
import ru.yandex.practicum.exception.EventNotFoundException;
import ru.yandex.practicum.exception.ServiceException;
import ru.yandex.practicum.service.CommentService;

import java.util.List;

@RestController
@RequestMapping("/events/{eventId}/comments")
@RequiredArgsConstructor
@Validated
public class CommentControllerPublic {

    private final CommentService commentService;

    @GetMapping
    public List<CommentDtoShort> findEventComments(
            @PathVariable @Positive Long eventId,
            @PageableDefault(page = 0, size = 10, sort = "created", direction = Sort.Direction.DESC) Pageable pageable) throws EventNotFoundException, ServiceException {
        return commentService.findEventComments(eventId, pageable);
    }
}