package ru.practicum.stats;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;
import ru.practicum.stats.config.StatsEndpointsProperties;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class ClientRestStatImpl implements ClientRestStat {

    private static final String QUERY_PARAM_START = "start";
    private static final String QUERY_PARAM_END = "end";
    private static final String QUERY_PARAM_UNIQUE = "unique";
    private static final String QUERY_PARAM_URIS = "uris";

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final String statsServerName;
    private final String fallbackUrl;
    private final RestClient restClient;
    private final DiscoveryClient discoveryClient;
    private final RetryTemplate retryTemplate;
    private final StatsEndpointsProperties endpoints;

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime.format(FORMATTER);
    }

    @Autowired
    public ClientRestStatImpl(RestClient restClient,
                              DiscoveryClient discoveryClient,
                              RetryTemplate retryTemplate,
                              @Value("${stats.server.service-name:stats-server}") String statsServerName,
                              @Value("${stats.server.fallback-url:http://localhost:9090}") String fallbackUrl, StatsEndpointsProperties endpoints) {
        this.restClient = restClient;
        this.discoveryClient = discoveryClient;
        this.retryTemplate = retryTemplate;
        this.statsServerName = statsServerName;
        this.fallbackUrl = fallbackUrl;
        this.endpoints = endpoints;
    }

    @Override
    public Boolean addStat(EndpointHitDto dto) {
        return retryTemplate.execute(context -> {
            URI statsServiceUri = buildStatsServiceUri(endpoints.getHit());
            log.debug("Calling stats service at: {}", statsServiceUri);
            return restClient.post()
                    .uri(statsServiceUri)
                    .body(dto)
                    .retrieve()
                    .body(Boolean.class);
        });
    }

    @Override
    public List<ViewStatsDto> getStat(LocalDateTime start,
                                      LocalDateTime end,
                                      List<String> uris,
                                      boolean unique) {
        return retryTemplate.execute(context -> {
            URI statsServiceUri = buildStatsServiceUri(endpoints.getStats(), start, end, uris, unique);
            log.debug("Calling stats-server at: {}", statsServiceUri);

            ResponseEntity<ViewStatsDto[]> responseEntity = restClient.get()
                    .uri(statsServiceUri)
                    .retrieve()
                    .toEntity(ViewStatsDto[].class);

            return responseEntity.getBody() != null ?
                    Arrays.asList(responseEntity.getBody()) :
                    Collections.emptyList();
        });
    }

    private URI buildStatsServiceUri(String path,
                                     LocalDateTime start,
                                     LocalDateTime end,
                                     List<String> uris,
                                     boolean unique) {
        String baseUrl = getStatsServiceBaseUrl();

        UriComponentsBuilder builder = UriComponentsBuilder
                .fromUriString(baseUrl)
                .path(path)
                .queryParam(QUERY_PARAM_START, formatDateTime(start))
                .queryParam(QUERY_PARAM_END, formatDateTime(end))
                .queryParam(QUERY_PARAM_UNIQUE, unique);

        if (uris != null && !uris.isEmpty()) {
            for (String uri : uris) {
                builder.queryParam(QUERY_PARAM_URIS, uri);
            }
        }

        return builder.build().toUri();
    }

    private URI buildStatsServiceUri(String path) {
        String baseUrl = getStatsServiceBaseUrl();

        return UriComponentsBuilder
                .fromUriString(baseUrl)
                .path(path)
                .build()
                .toUri();
    }

    /**
     * Получение url stats server через Discovery
     */
    private String getStatsServiceBaseUrl() {
        try {
            List<ServiceInstance> instances = discoveryClient.getInstances(statsServerName);

            if (instances != null && !instances.isEmpty()) {
                ServiceInstance instance = instances.get(0);
                String url = instance.getUri().toString();
                log.debug("Found stats-server instance: {}", url);
                return url;
            } else {
                log.warn("Stats-server '{}' not found in discovery. Using fallback: {}",
                        statsServerName, fallbackUrl);
                return fallbackUrl;
            }
        } catch (Exception e) {
            log.error("Error getting stats-server from discovery: {}", e.getMessage(), e);
            log.warn("Using fallback URL for stats-server: {}", fallbackUrl);
            return fallbackUrl;
        }
    }

}