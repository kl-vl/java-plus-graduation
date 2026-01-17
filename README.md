# Explore With Me - Сервис статистики с использованием микросервисной архитектуры и развёртыванием в облачных средах

Многомодульная архитектура:
```
explore-with-me/
├── infra/
│   ├── config-server/      # служба конфигурации
│   ├── discovery-server/   # служба обнаружения (Eureka)
│   └── gateway-server/     # служба API-шлюза
├── core/                   # группирующий модуль для основного сервиса:
│   └── main-service/       # основной сервис
└── stats/                  # Сервис статистики
    ├── stats-server/       # Web-сервер статистики (Web-приложение)
    ├── stats-client/       # HTTP-клиент для работы с main-сервисом через discovery-server
    └── stats-dto/          # Общие DTO классы статистики

```

- Spring Cloud Config Server — централизованное управление конфигурацией;
- Spring Cloud Eureka — сервис регистрации и обнаружения;
- Spring Cloud Gateway — API-шлюз для маршрутизации запросов.
- Stats-client with DiscoveryClient - http клиент для сервер статистики с динамическим определением url  

### Модули сервера статистики
- **stats/stats-server** - Web-сервер  для сбора и предоставления статистики
- **stats/stats-client** - HTTP-клиент для взаимодействия с сервером статистики
- **stats/stats-dto** - Общий формат данных (Data Transfer Objects)
