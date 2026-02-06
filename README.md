# Explore With Me (EWM) - приложение-афиша, сервис управления мероприятиями, статистики и рекомендаций для пользователей 

Микросервисная архитектура:
```
explore-with-me/
├── infra/
│   ├── config-server/      # служба конфигурации
│   ├── discovery-server/   # служба обнаружения (Eureka)
│   └── gateway-server/     # служба API-шлюза
├── core/                   # группирующий модуль для основного сервиса:
│   └── event-service/      # управление мероприятиями
│   └── user-service/       # управление пользователями
│   └── request-service/    # управление запросами на участие в мероприятиях (событиях)
│   └── comment-service/    # управление комментариями к мероприятиям
│   └── common/             # Feign-клиенты, DTO, исключения и общие конфигурации для сервисов
└── stats/                  # Сервис статистики
    ├── stats-client/       # GRPC-клиенты для работы с collector и analyzer серввисами
    ├── collector/          # сбор действий пользователей (gRPC → Kafka)
    ├── aggregator/         # агрегация действий и расчёт похожести событий (Kafka → Kafka)       
    └── analyzer/           # хранение статистики и выдача рекомендаций (Kafka + DB → gRPC)
```

- Spring Cloud Config Server — централизованное управление конфигурацией;
- Spring Cloud Eureka — сервис регистрации и обнаружения;
- Spring Cloud Gateway — API-шлюз для маршрутизации запросов.
- Stats-client with DiscoveryClient - http клиент для сервер статистики с динамическим определением url  


## Функциональность
- Управление пользователями - создание, просмотр и удаление пользователей
- Управление категориями - создание, редактирование и удаление категорий событий
- Управление событиями - создание, редактирование, публикация и просмотр событий
- Управление запросами - подача заявок на участие в событиях, подтверждение/отклонение заявок
- Управление комментариями - добавление, редактирование и удаление комментариев к событиям
- Управление подборками - создание и управление подборками событий
- Рекомендации и статистика взаимодействий - сбор действий пользователей (просмотры/регистрации/лайки), расчёт рейтингов и выдача рекомендаций

### Рекомендательный сервис состоит из 3 модулей 
- collector: принимает типы действий (ACTION_VIEW, ACTION_REGISTER, ACTION_LIKE) пользователей по gRPC и публикует сообщения в Kafka топик `stats.user-actions.v1`
- aggregator: читает топик `stats.user-actions.v1`, рассчитывает похожесть событий (score) и публикует результаты в `stats.events-similarity.v1`
- analyzer: читает топики `stats.user-actions.v1`, `stats.events-similarity.v1`), сохраняет данные в свою БД и отдаёт рекомендации (rating) по gRPC

### gRPC clients API (модуль stat-client)

- collector: UserActionGrpcController
    - collectUserAction (действия: VIEW/REGISTER/LIKE)
- analyzer: RecommendationsGrpcController
    - getRecommendationsForUser
    - getSimilarEvents
    - getInteractionsCount

## Запуск проекта

   ```bash
   docker compose up -d
   ```
Включает старт
- Kafka и создание топиков `stats.user-actions.v1` и `stats.events-similarity.v1`
- Создание контейнера PostgreSQL и скрипт(\docker\postgres\init-databases.sh) создания отдельных баз данных для каждого микросервиса
- Запуск `infra/discovery-server` (Eureka Server)
- Запуск `infra/config-server` (Spring Cloud Config Server)
- Запуск `infra/gateway-server` (API Gateway)
- Запуск `core/user-service` (Управление пользователями)
- Запуск `core/event-service` (Управление мероприятиями, категориями, подборками)
- Запуск `code/request-service` (Управление запросами на участие в мероприятиях)
- Запуск `code/comment-service` (Уплавление комментариями к мероприятиям)
- Запуск `stats/collector` (Сбор действий пользователей)
- Запуск `stats/aggregator` (Aгрегация действий и расчёт похожести событий)
- Запуск `stats/analyzer` (Хранение статистики и выдача рекомендаций)

Все cервисы работают на рэндомном порту и регистрируются в discovery-server, кроме eurika: 8761 и gateway: 8080. Каждый сервис
поддерживает работу actuator, даже если не содержит контролеров или обработки запросов на своем порту.

## Тестирование

Для тестирования включен набор postman коллекций тестов в папке [Postman](postman/) и отдельный [JAR файл тестера](postman/recommendations/tester-0.0.1.jar).
который имеет [собственное описание](postman/recommendations/Readme.md)


