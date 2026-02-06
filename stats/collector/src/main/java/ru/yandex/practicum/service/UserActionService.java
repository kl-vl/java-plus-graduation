package ru.yandex.practicum.service;

import ru.yandex.practicum.grpc.user.action.UserActionProto;

public interface UserActionService {

    void process(UserActionProto userAction);

}
