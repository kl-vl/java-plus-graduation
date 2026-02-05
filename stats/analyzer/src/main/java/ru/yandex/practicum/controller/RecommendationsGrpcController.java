package ru.yandex.practicum.controller;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import ru.yandex.practicum.grpc.collector.RecommendationsControllerGrpc;
import ru.yandex.practicum.grpc.similarity.reports.InteractionsCountRequestProto;
import ru.yandex.practicum.grpc.similarity.reports.RecommendedEventProto;
import ru.yandex.practicum.grpc.similarity.reports.SimilarEventsRequestProto;
import ru.yandex.practicum.grpc.similarity.reports.UserPredictionsRequestProto;
import ru.yandex.practicum.service.RecommendationService;

import java.util.List;
import java.util.function.Supplier;

@GrpcService
@RequiredArgsConstructor
@Slf4j
public class RecommendationsGrpcController extends RecommendationsControllerGrpc.RecommendationsControllerImplBase {

    private final RecommendationService recommendationService;

    @Override
    public void getRecommendationsForUser(UserPredictionsRequestProto request,
                                          StreamObserver<RecommendedEventProto> responseObserver) {
        processRequest(() -> recommendationService.getRecommendationsForUser(request), responseObserver);
    }

    @Override
    public void getSimilarEvents(SimilarEventsRequestProto request,
                                 StreamObserver<RecommendedEventProto> responseObserver) {
        processRequest(() -> recommendationService.getSimilarEvents(request), responseObserver);
    }

    @Override
    public void getInteractionsCount(InteractionsCountRequestProto request,
                                     StreamObserver<RecommendedEventProto> responseObserver) {
        processRequest(() -> recommendationService.getInteractionsCount(request), responseObserver);
    }

    private void processRequest(Supplier<List<RecommendedEventProto>> recommendationSupplier,
                                StreamObserver<RecommendedEventProto> responseObserver) {
        try {
            List<RecommendedEventProto> events = recommendationSupplier.get();
            for (RecommendedEventProto event : events) {
                responseObserver.onNext(event);
            }
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("RecommendationsGrpcController. Error processing gRPC request", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Internal server error")
                    .withCause(e)
                    .asRuntimeException());
        }
    }

}
