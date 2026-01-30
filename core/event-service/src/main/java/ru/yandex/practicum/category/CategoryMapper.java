package ru.yandex.practicum.category;

import org.mapstruct.Mapper;
import ru.yandex.practicum.config.CommonMapperConfiguration;
import ru.yandex.practicum.dto.category.CategoryDto;

@Mapper(config = CommonMapperConfiguration.class)
public interface CategoryMapper {

    Category toEntity(CategoryDto categoryDto);

    CategoryDto toDto(Category entity);
}
