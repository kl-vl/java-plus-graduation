package ru.yandex.practicum.client;

import org.springframework.cloud.openfeign.FeignClient;
import ru.yandex.practicum.api.EventClient;
import ru.yandex.practicum.client.fallback.EventClientFallBack;

@FeignClient(name = "${client.service.event.name:event-service}", path = "${client.service.event.path:/internal/events}", fallback = EventClientFallBack.class)
public interface EventFeignClient extends EventClient {
}
