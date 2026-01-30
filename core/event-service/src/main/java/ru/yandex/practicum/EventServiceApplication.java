package ru.yandex.practicum;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories(basePackages = "ru.yandex.practicum")
@EntityScan(basePackages = "ru.yandex.practicum")
@ComponentScan(basePackages = {"ru.yandex.practicum", "ru.practicum.stats"})
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "ru.yandex.practicum.client")
public class EventServiceApplication {

    public static void main(final String[] args) {
        SpringApplication.run(EventServiceApplication.class, args);
    }

}
