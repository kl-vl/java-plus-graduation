package ru.yandex.practicum.controller;

import com.google.protobuf.Empty;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import ru.yandex.practicum.grpc.collector.UserActionControllerGrpc;
import ru.yandex.practicum.grpc.user.action.UserActionProto;
import ru.yandex.practicum.service.UserActionService;

@GrpcService
@RequiredArgsConstructor
@Slf4j
public class UserActionGrpcController extends UserActionControllerGrpc.UserActionControllerImplBase {

    private final UserActionService userActionService;

    @Override
    public void collectUserAction(UserActionProto request, StreamObserver<Empty> responseObserver) {
        try {
            log.info("UserActionGrpcController. Collect user action UserId: {}, EventId:{}, ActionType: {}",
                    request.getUserId(), request.getEventId(), request.getActionType());

            userActionService.process(request);
            responseObserver.onNext(Empty.getDefaultInstance());
            responseObserver.onCompleted();
        } catch (Exception ex) {
            log.error("UserActionGrpcController. UserAction processing failed: {}", ex.getMessage(), ex);
            responseObserver.onError(new StatusRuntimeException(Status.INTERNAL.withDescription(ex.getMessage()).withCause(ex)));
        }
    }
}
