package tgb.cryptoexchange.orders.mapper;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import tgb.cryptoexchange.commons.enums.Merchant;
import tgb.cryptoexchange.grpc.generated.CreateOrderGrpc;
import tgb.cryptoexchange.grpc.generated.CreateOrderResponseGrpc;
import tgb.cryptoexchange.orders.dto.OrderDTO;
import tgb.cryptoexchange.orders.entity.Order;

import java.util.Objects;
import java.util.UUID;

@Component
public class OrderMapper {

    public OrderDTO toDTO(CreateOrderGrpc order) {
        return OrderDTO.builder()
                .clientId(order.getClientId())
                .internalId(order.getInternalId())
                .merchant(Merchant.valueOf(order.getMerchant()))
                .merchantOrderId(order.getMerchantOrderId())
                .merchantOrderStatus(order.getMerchantOrderStatus())
                .amount(order.getAmount())
                .enableUniqueAmount(order.getEnableUniqueAmount())
                .callbackUrl(order.hasCallbackUrl() ? order.getCallbackUrl() : null)
                .build();
    }

    public OrderDTO entityToDTO(Order order) {
        return OrderDTO.builder()
                .id(order.getId())
                .clientId(order.getClientId())
                .internalId(order.getInternalId())
                .status(order.getStatus())
                .amount(order.getAmount())
                .enableUniqueAmount(order.getEnableUniqueAmount())
                .callbackUrl(order.getCallbackUrl())
                .build();
    }

    public CreateOrderResponseGrpc createOrderResponseGrpc(OrderDTO orderDTO) {
        return CreateOrderResponseGrpc.newBuilder()
                .setId(Objects.nonNull(orderDTO.getId()) ? orderDTO.getId().toString() : StringUtils.EMPTY)
                .setClientId(Objects.nonNull(orderDTO.getClientId()) ? orderDTO.getClientId() : 0)
                .setInternalId(Objects.requireNonNullElse(orderDTO.getInternalId(), StringUtils.EMPTY))
                .setStatus(Objects.nonNull(orderDTO.getStatus()) ? orderDTO.getStatus().name() : StringUtils.EMPTY)
                .setAmount(Objects.nonNull(orderDTO.getAmount()) ? orderDTO.getAmount() : 0)
                .setEnableUniqueAmount(
                        Objects.nonNull(orderDTO.getEnableUniqueAmount()) && orderDTO.getEnableUniqueAmount())
                .setCallbackUrl(Objects.requireNonNullElse(orderDTO.getCallbackUrl(), StringUtils.EMPTY))
                .build();
    }

}
