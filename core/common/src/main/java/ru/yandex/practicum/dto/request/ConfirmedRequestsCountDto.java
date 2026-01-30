package ru.yandex.practicum.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmedRequestsCountDto implements ConfirmedRequestsCount {
    private Long eventId;
    private Long count;
}
