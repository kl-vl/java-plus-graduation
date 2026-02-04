package ru.yandex.practicum.controller;

import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;
import ru.yandex.practicum.grpc.collector.RecommendationsControllerGrpc;

@GrpcService
@RequiredArgsConstructor
public class RecommendationsGrpcController extends RecommendationsControllerGrpc.RecommendationsControllerImplBase {
    // TODO private final RecommendationService recommendationService;
}
