package tgb.cryptoexchange.orders.service;

import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.TimeBasedEpochGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tgb.cryptoexchange.orders.dto.OrderDTO;
import tgb.cryptoexchange.orders.entity.Order;
import tgb.cryptoexchange.orders.enums.OrderStatus;
import tgb.cryptoexchange.orders.exceptions.AlreadyExistsException;
import tgb.cryptoexchange.orders.exceptions.NotFoundException;
import tgb.cryptoexchange.orders.mapper.OrderMapper;
import tgb.cryptoexchange.orders.repository.OrderRepository;
import tgb.cryptoexchange.orders.utils.PageableUtils;

import java.util.List;
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

    /**
     * Создает новый order в системе с принудительной инициализацией статуса {@link OrderStatus#NEW}.
     *
     * @param orderDTO данные для создания нового order
     * @return {@link OrderDTO} созданного order с заполненным идентификатором и временем создания
     * @throws AlreadyExistsException если order с переданным {@code internalId} уже зарегистрирован в базе данных
     */
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

    /**
     * Обновляет статус существующего order и публикует событие об изменении его состояния.
     *
     * @param id        уникальный идентификатор order
     * @param newStatus новый устанавливаемый статус
     * @throws NotFoundException если order с указанным {@code id} не найден в базе данных
     */
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

    /**
     * Выполняет поиск order по заданной спецификации критериев с поддержкой пагинации и сортировки.
     *
     * @param spec    динамическая спецификация критериев фильтрации
     * @param page    номер запрашиваемой страницы
     * @param size    максимальное количество order на странице
     * @param sorters список строк правил сортировки (например, {@code "amount,desc"})
     * @return {@link Page} с найденными и преобразованными в DTO order
     */
    @Transactional(readOnly = true)
    public Page<OrderDTO> findOrders(Specification<Order> spec, int page, int size, List<String> sorters) {
        Pageable pageable = PageableUtils.createPageable(page, size, sorters);
        return orderRepository.findAll(spec, pageable).map(orderMapper::entityToDTO);
    }

    /**
     * Выполняет поиск всех order, соответствующих заданной спецификации критериев, без пагинации.
     * <p>
     *
     * @param spec динамическая спецификация критериев фильтрации
     * @return {@link List} со всеми найденными сущностями order
     */
    @Transactional(readOnly = true)
    public List<Order> findOrderByField(Specification<Order> spec){
        return orderRepository.findAll(spec);
    }

}
