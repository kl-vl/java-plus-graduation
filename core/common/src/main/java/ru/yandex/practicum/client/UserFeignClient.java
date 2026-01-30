package ru.yandex.practicum.client;

import org.springframework.cloud.openfeign.FeignClient;
import ru.yandex.practicum.api.UserClient;
import ru.yandex.practicum.client.fallback.UserClientFallback;

@FeignClient(name = "${client.service.user.name:user-service}", path = "${client.service.user.path:/internal/users}", fallback = UserClientFallback.class)
public interface UserFeignClient extends UserClient {
}
