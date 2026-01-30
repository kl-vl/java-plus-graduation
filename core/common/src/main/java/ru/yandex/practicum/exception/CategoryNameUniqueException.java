package ru.yandex.practicum.exception;

public class CategoryNameUniqueException extends Exception {
    public CategoryNameUniqueException(String message) {
        super(message);
    }
}
