package ru.practicum.stats.config;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "stats.server.endpoints")
@Getter
public class StatsEndpointsProperties {
    private final String hit = "/hit";
    private final String stats = "/stats";
}
