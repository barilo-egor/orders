package tgb.cryptoexchange.orders.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tgb.cryptoexchange.orders.enums.ClientStatus;

import java.time.Instant;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ClientDTO {

    private Long id;

    private String username;

    private String apiKeyPreview;

    private Instant registeredAt;

    private ClientStatus status;

    private String callbackUrl;

    private Integer orderTimeoutSeconds;

}
