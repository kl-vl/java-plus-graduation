package ru.yandex.practicum.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.yandex.practicum.config.CommonMapperConfiguration;
import ru.yandex.practicum.model.User;
import ru.yandex.practicum.dto.user.UserDto;

@Mapper(config = CommonMapperConfiguration.class)
public interface UserMapper {

    User toEntity(UserDto userDto);

    UserDto toUserDto(User entity);

    @Mapping(target = "email", ignore = true)
    UserDto toUserDtoShort(User entity);
}
