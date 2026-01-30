package ru.yandex.practicum.dto.validation;

public interface ValidationGroups {
    interface Create {}

    interface Update {}

    interface CreateAndUpdate extends Create, Update {}
}
