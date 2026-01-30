package ru.yandex.practicum.subscription.controller;

import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.event.Event;
import ru.yandex.practicum.subscription.model.Subscription;
import ru.yandex.practicum.subscription.model.SubscriptionDtoProjection;
import ru.yandex.practicum.subscription.service.SubscriptionService;
import ru.yandex.practicum.exception.ServiceException;
import ru.yandex.practicum.exception.UserNotFoundException;

import java.util.List;

@RestController
@RequestMapping("/users/{id}/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @PostMapping("/{subscription}")
    public Subscription create(
            @PathVariable(name = "id") @PositiveOrZero Long userId,
            @PathVariable(name = "subscription") @PositiveOrZero Long subscriptionId
    ) throws UserNotFoundException, ServiceException {
        return subscriptionService.create(userId, subscriptionId);
    }

    @DeleteMapping("/{subscription}")
    public void delete(
            @PathVariable(name = "id") @PositiveOrZero Long userId,
            @PathVariable(name = "subscription") @PositiveOrZero Long subscriptionId) {
        subscriptionService.delete(userId, subscriptionId);
    }

    @GetMapping
    public List<SubscriptionDtoProjection> getAllSubscriptions(
            @PathVariable(name = "id") @PositiveOrZero Long id
    ) {
        return subscriptionService.getAllSubscriptions(id);
    }

    @GetMapping("/events")
    public Page<Event> getAllEvents(
            @PathVariable(name = "id") @PositiveOrZero Long userId,
            Pageable pageable
    ) {
        return subscriptionService.getAllEvents(userId, pageable);
    }
}