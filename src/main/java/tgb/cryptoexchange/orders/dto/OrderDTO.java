package tgb.cryptoexchange.orders.dto;

import lombok.*;
import tgb.cryptoexchange.commons.enums.Merchant;
import tgb.cryptoexchange.orders.enums.OrderStatus;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class OrderDTO {

    private UUID id;

    private Long clientId;

    private String internalId;

    private Merchant merchant;

    private String merchantOrderId;

    private String merchantOrderStatus;

    private OrderStatus status;

    private Integer amount;

    private Boolean enableUniqueAmount;

    private String callbackUrl;

}
