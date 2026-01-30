package ru.yandex.practicum.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.yandex.practicum.dto.BooleanResponseDto;
import ru.yandex.practicum.dto.request.ConfirmedRequestsCountDto;
import ru.yandex.practicum.exception.EventNotFoundException;
import ru.yandex.practicum.exception.ServiceException;

import java.util.List;

public interface RequestClient {

    @GetMapping("/confirmed-count")
    ConfirmedRequestsCountDto getConfirmedRequestsCount(@RequestParam("eventId") Long eventId);

    @RequestMapping(value = "/participation/{userId}/{eventId}")
    BooleanResponseDto checkUserParticipation(@PathVariable Long userId, @PathVariable Long eventId) throws ServiceException, EventNotFoundException;

    @GetMapping("/confirmed-counts")
    List<ConfirmedRequestsCountDto> getConfirmedRequests(@RequestParam(name = "eventIds", required = true) List<Long> eventIds);

}
