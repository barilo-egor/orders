package tgb.cryptoexchange.orders.kafka;

import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.TimeBasedEpochGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import tgb.cryptoexchange.orders.dto.OrderConfirmationEventDTO;
import tgb.cryptoexchange.orders.dto.OrderDTO;
import tgb.cryptoexchange.orders.enums.Operation;
import tgb.cryptoexchange.orders.enums.OrderStatus;
import tgb.cryptoexchange.orders.enums.TransactionType;

@Component
@Slf4j
@Profile({ "!kafka-disabled" })
public class OrderConfirmationEventListener {

    private final KafkaTemplate<String, OrderConfirmationEventDTO> kafkaTemplate;

    private final String receiveTopicName;

    private final TimeBasedEpochGenerator generator = Generators.timeBasedEpochGenerator();

    public OrderConfirmationEventListener(KafkaTemplate<String, OrderConfirmationEventDTO> kafkaTemplate,
            @Value("${kafka.topic.orders.receive}") String receiveTopicName) {
        this.kafkaTemplate = kafkaTemplate;
        this.receiveTopicName = receiveTopicName;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCreatedEvent(OrderDTO orderDTO) {
        if (OrderStatus.SUCCESS.equals(orderDTO.getStatus())) {
            log.info("Транзакция успешно закоммичена. Пост-логика отправки kafka для заказа {}", orderDTO.getId());
            kafkaTemplate.send(receiveTopicName, new OrderConfirmationEventDTO(
                    generator.generate(),
                    orderDTO.getClientId(),
                    orderDTO.getAmount(),
                    Operation.CREDIT,
                    TransactionType.ORDER_CONFIRMATION,
                    orderDTO.getId()));
        }

    }

}
