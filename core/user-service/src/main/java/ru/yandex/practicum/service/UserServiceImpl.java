package ru.yandex.practicum.service;


import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.dto.BooleanResponseDto;
import ru.yandex.practicum.dto.user.UserDto;
import ru.yandex.practicum.exception.UserAlreadyExistsException;
import ru.yandex.practicum.exception.UserNotFoundException;
import ru.yandex.practicum.mapper.UserMapper;
import ru.yandex.practicum.model.User;
import ru.yandex.practicum.repository.UserRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    @Getter
    @Value("${spring.application.name}")
    private String serviceName;

    @Transactional(readOnly = true)
    public List<UserDto> findAllUsers(Pageable pageable, List<Long> ids) {
        log.info("{}. findAll input: pageble = {}, ids = {}", getServiceName(), pageable, ids);

        List<Long> idsForQuery = (ids == null || ids.isEmpty()) ? null : ids;
        Page<User> page = userRepository.findAllByIds(idsForQuery, pageable);

        log.info("{}. findAll success: found {} users", getServiceName(), page.getNumberOfElements());

//        return page.getContent().stream()
//                .map(user -> new UserDto(user.getId(), user.getEmail(), user.getName()))
//                .collect(Collectors.toList());
        return page.getContent().stream().map(userMapper::toUserDto).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public UserDto createUser(UserDto userDto) throws UserAlreadyExistsException {
        log.info("{}. createUser input: email = {}", getServiceName(), userDto.getEmail());

        if (userRepository.existsByEmail(userDto.getEmail())) {
            throw new UserAlreadyExistsException("User with email " + userDto.getEmail() + " already exists");
        }

        User createdUser = userRepository.save(userMapper.toEntity(userDto));

        log.info("{}. createUser success: id = {}", getServiceName(), createdUser.getId());

        return userMapper.toUserDto(createdUser);
    }

    @Override
    @Transactional
    public void deleteUserById(Long userId) {
        log.info("{}. deleteUserById input: userId = {}",getServiceName(),  userId);

        userRepository.deleteById(userId);

        log.info("{}. deleteUserById success", getServiceName());
    }

    @Override
    @Transactional(readOnly = true)
    public UserDto getUser(Long userId) throws UserNotFoundException {
        log.info("{}. getUser input: userId = {}", getServiceName(), userId);

        User user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));

        log.info("{}. getUser success: id = {}", getServiceName(), user);

        return userMapper.toUserDto(user);
    }

    @Override
    @Transactional(readOnly = true)
    public BooleanResponseDto existsUser(Long userId) {
        log.info("{}. existsUser input: userId = {}", getServiceName(), userId);

        boolean exists = userRepository.existsById(userId);

        log.info("{}. existsUser success: userId = {} {}", getServiceName(), userId, exists? "exists" : "does not exist");

        return new BooleanResponseDto(exists);
    }

}
