package tgb.cryptoexchange.orders.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import tgb.cryptoexchange.orders.dto.OrderConfirmationEventDTO;
import tgb.cryptoexchange.orders.kafka.OrderConfirmationEventSerializer;
import tgb.cryptoexchange.orders.kafka.OrderConfirmationProducerListener;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableAsync
@EnableScheduling
public class CommonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.registerModule(new JavaTimeModule());
        return objectMapper;
    }

    @Bean
    public WebClient.Builder webClientBuilder() {
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofSeconds(30));
        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient));
    }

    @Bean
    @Profile("!kafka-disabled")
    public ProducerFactory<String, OrderConfirmationEventDTO> orderConfirmationProducerFactory(
            KafkaProperties kafkaProperties, ObjectMapper objectMapper) {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBootstrapServers());
        return new DefaultKafkaProducerFactory<>(
                configProps,
                new StringSerializer(),
                new OrderConfirmationEventSerializer(objectMapper)
        );
    }

    @Bean
    @Profile("!kafka-disabled")
    public KafkaTemplate<String, OrderConfirmationEventDTO> kafkaTemplate(
            OrderConfirmationProducerListener orderConfirmationProducerListener,
            KafkaProperties kafkaProperties, ObjectMapper objectMapper) {
        KafkaTemplate<String, OrderConfirmationEventDTO> kafkaTemplate = new KafkaTemplate<>(
                orderConfirmationProducerFactory(kafkaProperties, objectMapper));
        kafkaTemplate.setProducerListener(orderConfirmationProducerListener);
        return kafkaTemplate;
    }

}
