package ru.yandex.practicum.subscription.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.event.Event;
import ru.yandex.practicum.event.EventRepository;
import ru.yandex.practicum.subscription.model.Subscription;
import ru.yandex.practicum.subscription.model.SubscriptionDtoProjection;
import ru.yandex.practicum.subscription.storage.SubscriptionRepository;
import ru.yandex.practicum.client.service.UserServiceClient;
import ru.yandex.practicum.exception.ServiceException;
import ru.yandex.practicum.exception.UserNotFoundException;

import java.util.List;

@RequiredArgsConstructor
@Service
@Slf4j
@Transactional(readOnly = true)
public class SubscriptionServiceImpl implements SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final EventRepository eventRepository;
    private final UserServiceClient userServiceClient;

    @Override
    public List<SubscriptionDtoProjection> getAllSubscriptions(Long id) {
        return subscriptionRepository.findByUserId(id);
    }

    @Override
    @Transactional
    public Subscription create(Long userId, Long subscriberId) throws UserNotFoundException, ServiceException {

        Subscription createSubscription = new Subscription();

        userServiceClient.validateUserExists(userId);
        userServiceClient.validateUserExists(subscriberId);

        createSubscription.setUserId(userId);
        createSubscription.setSubscriberId(subscriberId);

        return subscriptionRepository.save(createSubscription);
    }

    @Override
    @Transactional
    public void delete(Long userId, Long subscriptionId) {
        subscriptionRepository.deleteByUserIdAndSubscriberId(userId, subscriptionId);
    }

    @Override
    public Page<Event> getAllEvents(Long userId, Pageable pageable) {

        List<SubscriptionDtoProjection> subscriptions = getAllSubscriptions(userId);

        if (subscriptions.isEmpty()) {
            return Page.empty();
        }

        List<Long> subscribedUserIds = subscriptions.stream()
                .map(SubscriptionDtoProjection::getSubscriberId) // ← вот это работает!
                .distinct()
                .toList();

        log.debug("User {} subscribed to {} users: {}",
                userId, subscribedUserIds.size(), subscribedUserIds);

        return eventRepository.findAllByInitiatorIdIn(subscribedUserIds, pageable);
    }
}