package tgb.cryptoexchange.orders.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serializer;
import tgb.cryptoexchange.orders.dto.OrderConfirmationEventDTO;
import tgb.cryptoexchange.orders.exceptions.BodyMappingException;

@Slf4j
public class OrderConfirmationEventSerializer implements Serializer<OrderConfirmationEventDTO> {

    private final ObjectMapper objectMapper;

    public OrderConfirmationEventSerializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public byte[] serialize(String topic, OrderConfirmationEventDTO orderConfirmationEventDTO) {
        try {
            if (orderConfirmationEventDTO == null) {
                return new byte[0];
            }
            return objectMapper.writeValueAsBytes(orderConfirmationEventDTO);
        } catch (JsonProcessingException e) {
            log.error("Ошибка сериализации объекта для отправки в топик {}: {}", topic, orderConfirmationEventDTO);
            throw new BodyMappingException("Error occurred while mapping orderConfirmationEvent", e);
        }
    }
}
