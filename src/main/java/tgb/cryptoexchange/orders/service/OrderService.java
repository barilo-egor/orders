package tgb.cryptoexchange.orders.service;

import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.TimeBasedEpochGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tgb.cryptoexchange.orders.dto.OrderDTO;
import tgb.cryptoexchange.orders.entity.Order;
import tgb.cryptoexchange.orders.enums.OrderStatus;
import tgb.cryptoexchange.orders.exceptions.AlreadyExistsException;
import tgb.cryptoexchange.orders.exceptions.NotFoundException;
import tgb.cryptoexchange.orders.mapper.OrderMapper;
import tgb.cryptoexchange.orders.repository.OrderRepository;

import java.util.UUID;

@Service
@Slf4j
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;

    private final OrderMapper orderMapper;

    private final TimeBasedEpochGenerator generator = Generators.timeBasedEpochGenerator();

    private final ApplicationEventPublisher eventPublisher;

    public OrderService(OrderRepository orderRepository, OrderMapper orderMapper,
            ApplicationEventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.orderMapper = orderMapper;
        this.eventPublisher = eventPublisher;
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

        if (eventPublisher != null) {
            eventPublisher.publishEvent(orderMapper.entityToDTO(orderRepository.getOrdersById(id)));
        }
    }

}
