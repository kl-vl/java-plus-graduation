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

import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class ClientRestStatImpl implements ClientRestStat {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final String statsServerName;
    private final String fallbackUrl;

    private final RestClient restClient;
    private final DiscoveryClient discoveryClient;
    private final RetryTemplate retryTemplate;

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime.format(FORMATTER);
    }

    // TODO можно заавтоваирить discovery client
    //private final DiscoveryClient discoveryClient;
    //private final RetryTemplate retryTemplate;

    // TODO
/*    private ServiceInstance getInstance() {
        try {
            return discoveryClient
                    .getInstances(statsServiceId)
                    .getFirst();
        } catch (Exception exception) {
            throw new StatsServerUnavailable(
                    "Ошибка обнаружения адреса сервиса статистики с id: " + statsServiceId,
                    exception
            );
        }
    }*/

    // TODO метод наставника
//    private URI makeUri(String path) {
//        ServiceInstance instance = retryTemplate.execute(RetryContext context -> getInstance());
//        return URI.create("http://" + instance.getHost() + ":" + instance.getPort() + path);
//    }

//    public ClientRestStatImpl(RestClient restClient) {
//        this.restClient = restClient;
//    }

    @Autowired
    public ClientRestStatImpl(RestClient restClient,
                              DiscoveryClient discoveryClient,
                              RetryTemplate retryTemplate,
                              @Value("${stats.server.service-name:stats-server}") String statsServerName,
                              @Value("${stats.server.fallback-url:http://localhost:9090}") String fallbackUrl) {
        this.restClient = restClient;
        this.discoveryClient = discoveryClient;
        this.retryTemplate = retryTemplate;
        this.statsServerName = statsServerName;
        this.fallbackUrl = fallbackUrl;
    }

/*    @Override
    public Boolean addStat(EndpointHitDto dto) {
        return restClient.post()
                .uri("/hit")
                .body(dto)
                .retrieve()
                .body(Boolean.class);
    }*/

    @Override
    public Boolean addStat(EndpointHitDto dto) {
        return retryTemplate.execute(context -> {
            URI statsServiceUri = buildStatsServiceUri("/hit");
            log.debug("Calling stats service at: {}", statsServiceUri);
            return restClient.post()
                    .uri(statsServiceUri)
                    .body(dto)
                    .retrieve()
                    .body(Boolean.class);
        });
    }

/*    @Override
    public List<ViewStatsDto> getStat(LocalDateTime start, LocalDateTime end, List<String> uris, boolean unique) {
        URI uri = buildStatsUri(start, end, uris, unique);

        ResponseEntity<ViewStatsDto[]> responseEntity = restClient.get()
                .uri(uri)
                .retrieve()
                .toEntity(ViewStatsDto[].class);

        return responseEntity.getBody() != null ? Arrays.asList(responseEntity.getBody()) : Collections.emptyList();
    }*/


    @Override
    public List<ViewStatsDto> getStat(LocalDateTime start,
                                      LocalDateTime end,
                                      List<String> uris,
                                      boolean unique) {
        return retryTemplate.execute(context -> {
            URI statsServiceUri = buildStatsServiceUri("/stats", start, end, uris, unique);
            log.debug("Calling stats service at: {}", statsServiceUri);

            ResponseEntity<ViewStatsDto[]> responseEntity = restClient.get()
                    .uri(statsServiceUri)
                    .retrieve()
                    .toEntity(ViewStatsDto[].class);

            return responseEntity.getBody() != null ?
                    Arrays.asList(responseEntity.getBody()) :
                    Collections.emptyList();
        });
    }

//    private URI buildStatsUri(LocalDateTime start,
//                                  LocalDateTime end,
//                                  List<String> uris,
//                                  boolean unique) {
//            UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/stats")
//                    .queryParam("start", formatDateTime(start))
//                    .queryParam("end", formatDateTime(end))
//                    .queryParam("unique", unique);
//
//            if (uris != null && !uris.isEmpty()) {
//                for (String uri : uris) {
//                    builder.queryParam("uris", uri);
//                }
//            }
//            return builder.build().toUri();
//        }
//
//    private URI getStatsServiceUri(String path) {
//        List<ServiceInstance> instances = discoveryClient.getInstances(STATS_SERVER_NAME);
//
//        if (instances == null || instances.isEmpty()) {
//            throw new IllegalStateException("Сервис статистики недоступен: " + STATS_SERVER_NAME);
//        }
//
//        // первый доступный инстанс
//        ServiceInstance instance = instances.get(0);
//
//        return UriComponentsBuilder
//                .fromUriString(instance.getUri().toString())
//                .path(path)
//                .build()
//                .toUri();
//    }

    private URI buildStatsServiceUri(String path,
                                     LocalDateTime start,
                                     LocalDateTime end,
                                     List<String> uris,
                                     boolean unique) {
        String baseUrl = getStatsServiceBaseUrl();

        UriComponentsBuilder builder = UriComponentsBuilder
                .fromUriString(baseUrl)
                .path(path)
                .queryParam("start", formatDateTime(start))
                .queryParam("end", formatDateTime(end))
                .queryParam("unique", unique);

        if (uris != null && !uris.isEmpty()) {
            for (String uri : uris) {
                builder.queryParam("uris", uri);
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

    private String getStatsServiceBaseUrl() {
        try {
            // Пытаемся найти сервис через Discovery
            List<ServiceInstance> instances = discoveryClient.getInstances(statsServerName);

            if (instances != null && !instances.isEmpty()) {
                ServiceInstance instance = instances.get(0);
                String url = instance.getUri().toString();
                log.debug("Found stats service instance: {}", url);
                return url;
            } else {
                log.warn("Stats service '{}' not found in discovery. Using fallback: {}",
                        statsServerName, fallbackUrl);
                return fallbackUrl;
            }
        } catch (Exception e) {
            log.error("Error getting stats service from discovery: {}", e.getMessage(), e);
            log.warn("Using fallback URL for stats service: {}", fallbackUrl);
            return fallbackUrl;
        }
    }


/*
    private String buildStatsPath(LocalDateTime start,
                                  LocalDateTime end,
                                  List<String> uris,
                                  boolean unique) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/stats")
                .queryParam("start", formatDateTime(start))
                .queryParam("end", formatDateTime(end))
                .queryParam("unique", unique);

        if (uris != null && !uris.isEmpty()) {
            for (String uri : uris) {
                builder.queryParam("uris", uri);
            }
        }
        return builder.build().toUriString();
    }
*/

}