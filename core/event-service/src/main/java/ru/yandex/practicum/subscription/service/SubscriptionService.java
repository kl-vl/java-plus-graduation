package ru.yandex.practicum.subscription.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ru.yandex.practicum.event.Event;
import ru.yandex.practicum.subscription.model.Subscription;
import ru.yandex.practicum.subscription.model.SubscriptionDtoProjection;
import ru.yandex.practicum.exception.ServiceException;
import ru.yandex.practicum.exception.UserNotFoundException;

import java.util.List;

public interface SubscriptionService {

    Subscription create(Long subscription, Long user) throws UserNotFoundException, ServiceException;

    void delete(Long userId, Long subscriptionId);

    List<SubscriptionDtoProjection> getAllSubscriptions(Long id);

    Page<Event> getAllEvents(Long userId, Pageable pageable);
}