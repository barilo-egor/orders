package tgb.cryptoexchange.orders.service;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import tgb.cryptoexchange.grpc.generated.ClientsServiceGrpc;
import tgb.cryptoexchange.grpc.generated.GetClientByIdGrpc;
import tgb.cryptoexchange.grpc.generated.GetClientByIdResponseGrpc;
import tgb.cryptoexchange.orders.dto.ClientDTO;
import tgb.cryptoexchange.orders.exceptions.BaseException;
import tgb.cryptoexchange.orders.exceptions.UserNotFoundException;
import tgb.cryptoexchange.orders.mapper.ClientMapper;

@Service
@Slf4j
public class ApiClientsGrpcService {

    private final ClientMapper clientMapper;

    private final ClientsServiceGrpc.ClientsServiceFutureStub futureStub;

    public ApiClientsGrpcService(ClientsServiceGrpc.ClientsServiceFutureStub futureStub, ClientMapper clientMapper) {
        this.futureStub = futureStub;
        this.clientMapper = clientMapper;
    }

    public Mono<ClientDTO> getClientById(Long clientId) {
        log.debug("Реактивный gRPC запрос client: id {}", clientId);
        GetClientByIdGrpc request = GetClientByIdGrpc.newBuilder()
                .setId(clientId)
                .build();
        return Mono.fromFuture(() -> {
                    var guavaFuture = futureStub.getClientById(request);
                    var completableFuture = new java.util.concurrent.CompletableFuture<GetClientByIdResponseGrpc>();

                    Futures.addCallback(
                            guavaFuture,
                            new FutureCallback<>() {
                                @Override
                                public void onSuccess(GetClientByIdResponseGrpc result) {
                                    completableFuture.complete(result);
                                }

                                @Override
                                public void onFailure(@NonNull Throwable t) {
                                    completableFuture.completeExceptionally(t);
                                }
                            },
                            com.google.common.util.concurrent.MoreExecutors.directExecutor()
                    );
                    return completableFuture;
                })
                .map(clientMapper::clientByResponseToDTO)
                .onErrorResume(StatusRuntimeException.class, e -> {
                    if (e.getStatus().getCode() == Status.Code.NOT_FOUND) {
                        log.warn("Клиент с ID {} не найден через gRPC", clientId);
                        return Mono.error(new UserNotFoundException());
                    }
                    log.error("gRPC ошибка для клиента ID {}: {}", clientId, e.getStatus());
                    return Mono.error(new BaseException("Ошибка gRPC: " + e.getStatus().getDescription()));
                })
                .onErrorMap(e -> !(e instanceof UserNotFoundException || e instanceof BaseException),
                        e -> new BaseException("Критическая ошибка gRPC: " + e.getMessage()));
    }

}
