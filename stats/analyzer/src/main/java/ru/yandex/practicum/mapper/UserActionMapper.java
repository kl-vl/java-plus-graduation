package ru.yandex.practicum.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.yandex.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.yandex.practicum.ewm.stats.avro.UserActionAvro;
import ru.yandex.practicum.model.ActionType;
import ru.yandex.practicum.model.UserAction;

@Mapper(componentModel = "spring")
public interface UserActionMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(source = "actionType", target = "actionType")
    @Mapping(target = "actionWeight", expression = "java(calculateActionWeight(request.getActionType()))")
    UserAction toEntity(UserActionAvro request);

    default ActionType map(ActionTypeAvro actionType) {
        if (actionType == null) {
            return null;
        }
        return ActionType.valueOf(actionType.name());
    }

    /*default Double calculateActionWeight(ActionTypeAvro actionType) {
        if (actionType == null) {
            return 0.0;
        }

        return switch (actionType) {
            case VIEW -> 0.4;
            case REGISTER -> 0.8;
            case LIKE -> 1.0;
        };
    }*/
    default Double calculateActionWeight(ActionTypeAvro actionType) {
        ActionType domainType = ActionType.fromAvro(actionType);
        return ActionType.getWeightOrDefault(domainType, 0.0);
    }
}
