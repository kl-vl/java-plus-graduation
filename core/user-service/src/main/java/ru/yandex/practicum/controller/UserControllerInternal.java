package ru.yandex.practicum.controller;

import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.api.UserClient;
import ru.yandex.practicum.dto.BooleanResponseDto;
import ru.yandex.practicum.dto.user.UserDto;
import ru.yandex.practicum.exception.UserNotFoundException;
import ru.yandex.practicum.service.UserService;

import java.util.List;

@RestController
@RequestMapping("/internal/users")
@RequiredArgsConstructor
@Validated
public class UserControllerInternal implements UserClient {

    private final UserService userService;

    @Value("${pagination.user.max-size:1000}")
    private Integer maxSize;

    @Override
    @GetMapping("/{userId}")
    public UserDto getUser(@PathVariable @Positive Long userId) throws UserNotFoundException {
        return userService.getUser(userId);
    }

    @Override
    @GetMapping("/{userId}/exists")
    public BooleanResponseDto existsUser(@PathVariable @Positive Long userId) {
        return userService.existsUser(userId);
    }

    @Override
    @GetMapping
    public List<UserDto> getUsers(@RequestParam(name = "userIds", required = false) List<Long> userIds) {
        Pageable pageable = PageRequest.of(0, maxSize);
        return userService.findAllUsers(pageable, userIds);
    }
}
