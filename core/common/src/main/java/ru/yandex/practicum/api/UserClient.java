package ru.yandex.practicum.api;

import jakarta.validation.constraints.Positive;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import ru.yandex.practicum.dto.BooleanResponseDto;
import ru.yandex.practicum.dto.user.UserDto;
import ru.yandex.practicum.exception.UserNotFoundException;

import java.util.List;

public interface UserClient {

    @GetMapping("/{userId}")
    UserDto getUser(@PathVariable @Positive Long userId) throws UserNotFoundException;

    @GetMapping("/{userId}/exists")
    BooleanResponseDto existsUser(@PathVariable @Positive Long userId);

    @GetMapping
    List<UserDto> getUsers(@RequestParam(name = "userIds", required = false) List<Long> userIds);
}
