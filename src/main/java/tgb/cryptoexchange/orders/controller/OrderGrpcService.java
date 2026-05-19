package tgb.cryptoexchange.orders.controller;

import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.grpc.server.service.GrpcService;
import tgb.cryptoexchange.grpc.generated.CreateOrderGrpc;
import tgb.cryptoexchange.grpc.generated.CreateOrderResponseGrpc;
import tgb.cryptoexchange.grpc.generated.OrdersServiceGrpc;
import tgb.cryptoexchange.grpc.generated.UpdateOrderStatusGrpc;
import tgb.cryptoexchange.orders.dto.OrderDTO;
import tgb.cryptoexchange.orders.enums.OrderStatus;
import tgb.cryptoexchange.orders.mapper.OrderMapper;
import tgb.cryptoexchange.orders.service.OrderService;

import java.util.UUID;

@GrpcService
@Slf4j
public class OrderGrpcService extends OrdersServiceGrpc.OrdersServiceImplBase {

    private final OrderMapper orderMapper;

    private final OrderService orderService;

    public OrderGrpcService(OrderMapper orderMapper, OrderService orderService) {
        this.orderMapper = orderMapper;
        this.orderService = orderService;
    }

    @Override
    public void createOrder(CreateOrderGrpc request, StreamObserver<CreateOrderResponseGrpc> responseObserver) {
        OrderDTO orderDTO = orderMapper.toDTO(request);
        OrderDTO savedOrder = orderService.create(orderDTO);
        responseObserver.onNext(orderMapper.createOrderResponseGrpc(savedOrder));
        responseObserver.onCompleted();
    }

    @Override
    public void updateOrderStatus(UpdateOrderStatusGrpc request, StreamObserver<Empty> responseObserver){
        orderService.updateStatus(UUID.fromString(request.getId()), OrderStatus.valueOf(request.getStatus()));
    }

}
