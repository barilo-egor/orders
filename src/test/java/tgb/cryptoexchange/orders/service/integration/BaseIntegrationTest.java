package tgb.cryptoexchange.orders.service.integration;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.grpc.test.autoconfigure.LocalGrpcPort;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;
import org.wiremock.spring.ConfigureWireMock;
import org.wiremock.spring.InjectWireMock;
import tgb.cryptoexchange.orders.repository.OrderRepository;

@ActiveProfiles("test")
@SpringBootTest(properties = "grpc.server.port=-1")
@RecordApplicationEvents
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(WireMockConfiguration.class)
@ConfigureWireMock(
        name = "client-service",
        baseUrlProperties = { "app.webclient.base-url" }
)
@Testcontainers
public abstract class BaseIntegrationTest {

    static final MySQLContainer<?> mysql;

    static final KafkaContainer kafka;

    static {
        mysql = new MySQLContainer<>("mysql:8.0")
                .withDatabaseName("testdb")
                .withReuse(true);

        kafka = new KafkaContainer(DockerImageName.parse("apache/kafka:3.7.0"));

        mysql.start();
        kafka.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);

        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @InjectWireMock("client-service")
    protected WireMockServer wireMockServer;

    protected WireMock wireMockClient;

    @LocalGrpcPort
    protected int port;

    protected ManagedChannel channel;

    @Autowired
    protected OrderRepository orderRepository;

    @Autowired
    protected KafkaProperties kafkaProperties;

    @BeforeEach
    void initWireMockClient() {
        wireMockClient = new WireMock("localhost", wireMockServer.port());
        wireMockClient.resetMappings();
    }

    @BeforeEach
    void initChannel() {
        channel = ManagedChannelBuilder.forAddress("localhost", port)
                .usePlaintext()
                .build();
    }

    @BeforeEach
    @Transactional
    void clearDatabase() {
        orderRepository.deleteAllInBatch();
    }

    @AfterEach
    void tearDown() {
        if (channel != null) {
            channel.shutdownNow();
        }
    }

}
