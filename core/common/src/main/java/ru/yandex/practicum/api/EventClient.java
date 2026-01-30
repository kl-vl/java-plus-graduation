package ru.yandex.practicum.api;

import jakarta.validation.constraints.Positive;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import ru.yandex.practicum.dto.BooleanResponseDto;
import ru.yandex.practicum.dto.event.EventDtoFull;
import ru.yandex.practicum.exception.EventNotFoundException;

public interface EventClient {

    @GetMapping("/{eventId}")
    EventDtoFull getEvent(@PathVariable @Positive Long eventId) throws EventNotFoundException;

    @GetMapping("/{eventId}/initiator/{userId}")
    EventDtoFull getEventByInitiator(
            @PathVariable @Positive Long eventId,
            @PathVariable @Positive Long userId
    )  throws EventNotFoundException;

    @GetMapping("/{eventId}/exists")
    BooleanResponseDto existsEvent(@PathVariable @Positive Long eventId);

}
