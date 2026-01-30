package ru.yandex.practicum.exception;

public class ParticipantLimitExceededException extends Exception {
    public ParticipantLimitExceededException(String message) {
        super(message);
    }
}
