package ru.yandex.practicum.request.controller;

import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.api.RequestClient;
import ru.yandex.practicum.dto.BooleanResponseDto;
import ru.yandex.practicum.dto.request.ConfirmedRequestsCountDto;
import ru.yandex.practicum.request.service.RequestService;

import java.util.List;

@RestController
@RequestMapping("/internal/requests")
@RequiredArgsConstructor
@Validated
public class RequestControllerInternal implements RequestClient {

    private final RequestService requestService;

    @Override
    @RequestMapping(value = "/participation/{userId}/{eventId}")
    public BooleanResponseDto checkUserParticipation(Long userId, Long eventId) {
        return requestService.checkUserParticipation(eventId, userId);
    }


    @Override
    @GetMapping("/confirmed-count")
    public ConfirmedRequestsCountDto getConfirmedRequestsCount(@RequestParam("eventId") Long eventId) {
        return requestService.getConfirmedRequestsCount(eventId);
    }

    @Override
    @GetMapping("/confirmed-counts")
    public List<ConfirmedRequestsCountDto> getConfirmedRequests(@RequestParam(name = "eventIds", required = false) List<@Positive Long> eventIds) {
        return requestService.getConfirmedRequests(eventIds);
    }

}
