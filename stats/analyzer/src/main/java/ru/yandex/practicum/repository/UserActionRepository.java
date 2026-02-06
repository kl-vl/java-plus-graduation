package ru.yandex.practicum.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.model.UserAction;

import java.util.List;

@Repository
public interface UserActionRepository extends JpaRepository<UserAction, Long> {
    List<UserAction> findAllByUserId(Long userId);

    List<UserAction> findByEventIdIn(List<Long> eventIds);

    @Query("SELECT a FROM UserAction a WHERE a.id IN " +
            "(SELECT MAX(ua.id) FROM UserAction ua " +
            "WHERE ua.eventId IN :eventIds " +
            "GROUP BY ua.eventId, ua.userId)")
    List<UserAction> findMaxWeightedByEventIds(@Param("eventIds") List<Long> eventIds);

}


