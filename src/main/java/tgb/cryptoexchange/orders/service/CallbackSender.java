package tgb.cryptoexchange.orders.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import tgb.cryptoexchange.orders.dto.OrderDTO;

import java.time.Duration;

@Slf4j
@Service
public class CallbackSender {

    private final WebClient webClient;

    public CallbackSender() {
        this.webClient = WebClient.builder().build();
    }

    @Async
    public void sendPostOrderStatusUpdate(OrderDTO orderDTO) {
        if (StringUtils.isBlank(orderDTO.getCallbackUrl())) {
            return;
        }
        log.info("Запуск отправки вебхука для order {}", orderDTO.getId());
        try {
            webClient.post()
                    .uri(orderDTO.getCallbackUrl())
                    .bodyValue(orderDTO)
                    .retrieve()
                    .toBodilessEntity()
                    .timeout(Duration.ofSeconds(5))
                    .block();

            log.info("Callback для order {} успешно отправлен по адресу {}", orderDTO.getId(), orderDTO.getCallbackUrl());
        } catch (Exception e) {
            log.error("Не удалось доставить callback на {} для order {}: {}", orderDTO.getCallbackUrl(),
                    orderDTO.getId(), e.getMessage());
        }
    }

}
