package tgb.cryptoexchange.orders.config;

import io.grpc.Channel;
import io.grpc.ClientInterceptors;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.client.GrpcChannelFactory;
import tgb.cryptoexchange.grpc.generated.ClientsServiceGrpc;
import tgb.cryptoexchange.web.AuthLoginService;

@Configuration
public class GrpcClientConfig {

    @Bean
    public ClientsServiceGrpc.ClientsServiceFutureStub clientsServiceFutureStub(GrpcChannelFactory channelFactory,
            AuthLoginService authLoginService) {
        Channel channel = channelFactory.createChannel("api-clients");
        Channel interceptingChannel = ClientInterceptors.intercept(
                channel,
                new AuthGrpcInterceptor(authLoginService)
        );
        return ClientsServiceGrpc.newFutureStub(interceptingChannel);
    }

}
