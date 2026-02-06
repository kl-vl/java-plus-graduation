package ru.yandex.practicum.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.model.EventSimilarity;

import java.util.List;

@Repository
public interface SimilarityRepository extends JpaRepository<EventSimilarity, Long> {

    List<EventSimilarity> findByEventA(Long eventId);
    List<EventSimilarity> findByEventB(Long eventId);
    List<EventSimilarity> findByEventAIn(List<Long> eventIds);
    List<EventSimilarity> findByEventBIn(List<Long> eventIds);
    EventSimilarity findByEventAAndEventB(Long eventA, Long eventB);

    long countByEventA(Long eventA);

    long countByEventB(Long eventB);

    @Query("SELECT s FROM EventSimilarity s WHERE s.eventA = :eventId OR s.eventB = :eventId ORDER BY s.score DESC")
    List<EventSimilarity> findTopByEventId(@Param("eventId") Long eventId, Pageable pageable);

}
