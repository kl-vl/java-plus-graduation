package ru.yandex.practicum.client;

import org.springframework.cloud.openfeign.FeignClient;
import ru.yandex.practicum.api.RequestClient;
import ru.yandex.practicum.client.fallback.RequestClientFallback;

@FeignClient(name = "${client.service.request.name:request-service}", path = "${client.service.request.path:/internal/requests}", fallback = RequestClientFallback.class)
public interface RequestFeignClient extends RequestClient {
}
