package ru.yandex.practicum.exception;

public class EventNotFoundException extends Exception {
    public EventNotFoundException(String message) {
        super(message);
    }

    public EventNotFoundException(Long eventId) {
        super("Event not found: " + eventId);
    }
}
