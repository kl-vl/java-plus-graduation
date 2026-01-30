package ru.yandex.practicum.subscription.storage;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import ru.yandex.practicum.subscription.model.Subscription;
import ru.yandex.practicum.subscription.model.SubscriptionDtoProjection;

import java.util.List;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    List<SubscriptionDtoProjection> findByUserId(Long id);

    @Modifying
    void deleteByUserIdAndSubscriberId(Long userId, Long subscriptionId);
}