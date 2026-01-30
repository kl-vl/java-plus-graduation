package ru.yandex.practicum.service;

import org.springframework.data.domain.Pageable;
import ru.yandex.practicum.dto.BooleanResponseDto;
import ru.yandex.practicum.dto.user.UserDto;
import ru.yandex.practicum.exception.UserAlreadyExistsException;
import ru.yandex.practicum.exception.UserNotFoundException;

import java.util.List;

public interface UserService {
    UserDto createUser(UserDto userDto) throws UserAlreadyExistsException;

    void deleteUserById(Long userId);

    List<UserDto> findAllUsers(Pageable pageable, List<Long> ids);

    UserDto getUser(Long userId) throws UserNotFoundException;

    BooleanResponseDto existsUser(Long userId);
}
