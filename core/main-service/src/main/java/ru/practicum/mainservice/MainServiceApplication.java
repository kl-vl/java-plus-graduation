package ru.practicum.mainservice;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories(basePackages = "ru.practicum.mainservice")
@EntityScan(basePackages = "ru.practicum.mainservice")
@ComponentScan(basePackages = {"ru.practicum.mainservice", "ru.practicum.stats"})
@EnableDiscoveryClient
public class MainServiceApplication {

    @Value("${app.startup-message:NOT SET}")
    private String startupMessage;

    public static void main(final String[] args) {
        SpringApplication.run(MainServiceApplication.class, args);
    }

    // TODO del
    @PostConstruct
    public void logConfigSource() {
        System.out.println(">>> CONFIG MESSAGE: " + startupMessage);
    }

}
