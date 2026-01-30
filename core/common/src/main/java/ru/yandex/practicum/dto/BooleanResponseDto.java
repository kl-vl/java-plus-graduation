package ru.yandex.practicum.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BooleanResponseDto {

    private boolean result;
    private String message;

    public BooleanResponseDto(boolean result) {
        this.result = result;
        this.message = result ? "true" : "false";
    }

}
