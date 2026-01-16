package ru.practicum.stats.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.NeverRetryPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.RestClient;
import ru.practicum.stats.ClientRestStat;
import ru.practicum.stats.ClientRestStatImpl;

@Configuration
public class ClientRestStatConfig {

    @Bean
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }

    @Bean
    public RestClient statsRestClient(RestClient.Builder restClientBuilder) {
        return restClientBuilder.build();
    }

    @Bean
    public RetryTemplate statsRetryTemplate(
            @Value("${stats.retry.enabled:true}") boolean retryEnabled,
            @Value("${stats.retry.max-attempts:3}") int maxAttempts) {

        RetryTemplate retryTemplate = new RetryTemplate();

        if (retryEnabled) {
            SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
            retryPolicy.setMaxAttempts(maxAttempts);
            retryTemplate.setRetryPolicy(retryPolicy);

            // TODO в настройки
            ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
            backOffPolicy.setInitialInterval(1000);
            backOffPolicy.setMultiplier(2.0);
            backOffPolicy.setMaxInterval(10000);
            retryTemplate.setBackOffPolicy(backOffPolicy);
        } else {
            retryTemplate.setRetryPolicy(new NeverRetryPolicy());
        }

        return retryTemplate;
    }

    @Bean
    public ClientRestStat clientRestStat(RestClient statsRestClient,
                                         DiscoveryClient discoveryClient,
                                         RetryTemplate statsRetryTemplate,
                                         @Value("${stats.server.service-name:stats-server}") String serviceName,
                                         @Value("${stats.server.fallback-url:http://localhost:9090}") String fallbackUrl) {
        return new ClientRestStatImpl(statsRestClient, discoveryClient, statsRetryTemplate, serviceName, fallbackUrl);
    }

}