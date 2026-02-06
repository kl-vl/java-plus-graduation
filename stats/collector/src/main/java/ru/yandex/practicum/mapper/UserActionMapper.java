package ru.yandex.practicum.mapper;

import com.google.protobuf.Timestamp;
import org.mapstruct.Mapper;
import ru.yandex.practicum.grpc.user.action.ActionTypeProto;
import ru.yandex.practicum.grpc.user.action.UserActionProto;
import ru.yandex.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.yandex.practicum.ewm.stats.avro.UserActionAvro;

import java.time.Instant;

@Mapper(componentModel = "spring")
public interface UserActionMapper {

    UserActionAvro mapToAvro(UserActionProto action);
    default ActionTypeAvro mapActionType(ActionTypeProto actionType) {
        if (actionType == null) {
            throw new IllegalArgumentException("UserActionProto cannot be null.");
        }
        return switch (actionType) {
            case ACTION_VIEW -> ActionTypeAvro.VIEW;
            case ACTION_REGISTER -> ActionTypeAvro.REGISTER;
            case ACTION_LIKE -> ActionTypeAvro.LIKE;
            default -> throw new IllegalArgumentException("Unknown UserActionProto: " + actionType);
        };
    }

    default Instant map(Timestamp timestamp) {
        if (timestamp == null) {
            return null;
        }
        return Instant.ofEpochSecond(
                timestamp.getSeconds(),
                timestamp.getNanos()
        );
    }
}