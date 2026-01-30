package ru.yandex.practicum.client.fallback;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.api.UserClient;
import ru.yandex.practicum.dto.BooleanResponseDto;
import ru.yandex.practicum.dto.user.UserDto;
import ru.yandex.practicum.exception.UserNotFoundException;

import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class UserClientFallback implements UserClient {

    @Override
    public UserDto getUser(Long userId) throws UserNotFoundException {
        log.warn("UserClient fallback triggered for getUser, userId={}", userId);
        throw new UserNotFoundException("User-service is temporary unavailable. userId=" + userId);
    }

    @Override
    public BooleanResponseDto existsUser(Long userId) {
        log.warn("UserClient fallback triggered for existsUser, userId={}", userId);
        return BooleanResponseDto.builder()
                .result(false)
                .message("User-service is temporary unavailable")
                .build();
    }

    @Override
    public List<UserDto> getUsers(List<Long> userIds) {
        log.warn("UserClient fallback triggered for getUsers, userIds={}", userIds);
        return new ArrayList<>();
    }
}
