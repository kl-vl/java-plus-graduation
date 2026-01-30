package ru.yandex.practicum.exception;

public class EventAlreadyPublishedException extends Exception {
    public EventAlreadyPublishedException(String message) {
        super(message);
    }
}
