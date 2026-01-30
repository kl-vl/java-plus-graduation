package ru.yandex.practicum.dto.event;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.List;

/** public + admin
 * список идентификаторов категорий в которых будет вестись поиск
 */
@SuperBuilder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class EventFilterBase {
    private List<Long> categories;

    // public + admin
    private LocalDateTime rangeStart;

    // public + admin
    private LocalDateTime rangeEnd;

    // public + admin
    @PositiveOrZero
    @Builder.Default
    private int from = 0;

    // public + admin
    @Min(value = 1)
    @Max(value = 1000)
    @Builder.Default
    private int size = 10;
}
