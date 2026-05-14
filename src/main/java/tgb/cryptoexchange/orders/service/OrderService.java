package tgb.cryptoexchange.orders.service;

import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.TimeBasedEpochGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tgb.cryptoexchange.orders.dto.OrderDTO;
import tgb.cryptoexchange.orders.entity.Order;
import tgb.cryptoexchange.orders.enums.OrderStatus;
import tgb.cryptoexchange.orders.exceptions.AlreadyExistsException;
import tgb.cryptoexchange.orders.exceptions.NotFoundException;
import tgb.cryptoexchange.orders.mapper.OrderMapper;
import tgb.cryptoexchange.orders.repository.OrderRepository;

import java.util.Objects;
import java.util.UUID;

@Service
@Slf4j
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;

    private final OrderMapper orderMapper;

    private final CallbackSender callbackSender;

    private final TimeBasedEpochGenerator generator = Generators.timeBasedEpochGenerator();

    public OrderService(OrderRepository orderRepository, OrderMapper orderMapper, CallbackSender callbackSender) {
        this.orderRepository = orderRepository;
        this.orderMapper = orderMapper;
        this.callbackSender = callbackSender;
    }

    public OrderDTO create(OrderDTO orderDTO) {
        log.debug("Запрос на создание order: {}", orderDTO);
        if (orderRepository.existsByInternalId(orderDTO.getInternalId())) {
            throw new AlreadyExistsException(orderDTO.getInternalId());
        }
        Order order = Order.builder()
                .id(generator.generate())
                .clientId(orderDTO.getClientId())
                .internalId(orderDTO.getInternalId())
                .status(OrderStatus.NEW)
                .amount(orderDTO.getAmount())
                .enableUniqueAmount(orderDTO.getEnableUniqueAmount())
                .callbackUrl(orderDTO.getCallbackUrl())
                .build();

        order = orderRepository.save(order);
        log.debug("Создан order: {}", order.getId());
        return orderMapper.entityToDTO(order);
    }

    public void updateStatus(UUID id, OrderStatus newStatus) {
        log.debug("Запрос на обновление статуса order, id={}, newStatus={}", id, newStatus);
        int result = orderRepository.updateStatusById(id, newStatus);
        if (result == 0) {
            throw new NotFoundException(id.toString());
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    log.info("Транзакция успешно закоммичена. Пост-логика для заказа {}", id);
                    executePostOrderStatusUpdate(id, newStatus);
                }
            });
        }
    }

    private void executePostOrderStatusUpdate(UUID id, OrderStatus newStatus) {
        Order order = orderRepository.getOrdersById(id);
        OrderDTO orderDTO = orderMapper.entityToDTO(order);
        if (Objects.isNull(orderDTO.getCallbackUrl())) {

        }
        callbackSender.sendPostOrderStatusUpdate(orderDTO);
    }

}
