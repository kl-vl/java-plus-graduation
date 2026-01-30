package ru.yandex.practicum.category;

import ru.yandex.practicum.dto.category.CategoryDto;
import ru.yandex.practicum.exception.CategoryIsRelatedToEventException;
import ru.yandex.practicum.exception.CategoryNameUniqueException;
import ru.yandex.practicum.exception.CategoryNotFoundException;
import ru.yandex.practicum.exception.InvalidCategoryException;

import java.util.List;

public interface CategoryService {
    CategoryDto createCategory(CategoryDto categoryDto) throws CategoryNameUniqueException, InvalidCategoryException;

    CategoryDto updateCategory(CategoryDto categoryDto) throws CategoryNotFoundException, CategoryNameUniqueException, InvalidCategoryException;

    boolean deleteCategory(Long catId) throws CategoryIsRelatedToEventException;

    CategoryDto findCategoryById(Long catId) throws CategoryNotFoundException;

    List<CategoryDto> findAllCategories(Integer from, Integer size);
}
