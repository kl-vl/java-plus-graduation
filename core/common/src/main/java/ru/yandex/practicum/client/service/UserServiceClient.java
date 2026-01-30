package ru.yandex.practicum.client.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.api.UserClient;
import ru.yandex.practicum.dto.BooleanResponseDto;
import ru.yandex.practicum.dto.user.UserDto;
import ru.yandex.practicum.exception.ServiceException;
import ru.yandex.practicum.exception.UserNotFoundException;

import java.util.Collections;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class UserServiceClient {
    private final UserClient userClient;

    public void validateUserExists(Long userId) throws UserNotFoundException,ServiceException {
        log.info("Validating user existence: userId = {}", userId);

        try {
            BooleanResponseDto existsResponse = userClient.existsUser(userId);
            if (!existsResponse.isResult()) {
                throw new UserNotFoundException(
                        String.format("User with id %d not found. Reason: %s",
                                userId, existsResponse.getMessage())
                );
            }
            log.info("User validation successful: userId = {}", userId);

        } catch (UserNotFoundException e) {
            throw e;

        } catch (Exception e) {
            log.warn("User-service call failed for userId {} : {}", userId, e.getMessage(), e);
            throw new ServiceException("User-service unavailable.", e);
        }
    }

    public UserDto fetchUserDto(Long userId) throws UserNotFoundException, ServiceException {
        log.info("Fetching user: userId = {}", userId);
        try {
            UserDto userDto = userClient.getUser(userId);
            log.info("User fetching successful: userId = {}", userDto.getEmail());
            return userDto;
        } catch (UserNotFoundException e) {
            throw new UserNotFoundException(userId);
        } catch (Exception e) {
            log.error("User-service call failed for userId {}: {}", userId, e.getMessage(), e);
            throw new ServiceException("User-service unavailable", e);
        }
    }

    public List<UserDto> fetchUsers(List<Long> userIds) {
        log.info("Fetching {} users", userIds != null ? userIds.size() : 0);

        try {
            return userClient.getUsers(userIds);

        } catch (Exception e) {
            log.error("Failed to fetch users: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

}
