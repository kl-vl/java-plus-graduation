package ru.yandex.practicum.dto.category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import ru.yandex.practicum.dto.validation.ValidationGroups;

@Builder
@Getter
@Setter
public class CategoryDto {

    @Null(groups = ru.yandex.practicum.dto.validation.ValidationGroups.Create.class)
    private Long id;

    @NotNull(groups = ValidationGroups.Create.class)
    @NotBlank
    @Size(min = 1, max = 50)
    private String name;

}
