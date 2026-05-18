package tgb.cryptoexchange.orders.config;

import io.grpc.*;
import lombok.RequiredArgsConstructor;
import tgb.cryptoexchange.web.AuthLoginService;

@RequiredArgsConstructor
public class AuthGrpcInterceptor implements ClientInterceptor {

    private final AuthLoginService authLoginService;

    private static final Metadata.Key<String> AUTHORIZATION_KEY =
            Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER);

    @Override
    public <R, T> ClientCall<R, T> interceptCall(
            MethodDescriptor<R, T> method,
            CallOptions callOptions,
            Channel next) {

        return new ForwardingClientCall.SimpleForwardingClientCall<>(next.newCall(method, callOptions)) {
            @Override
            public void start(Listener<T> responseListener, Metadata headers) {
                String token = authLoginService.login();
                if (token != null && !token.isEmpty()) {
                    headers.put(AUTHORIZATION_KEY, "Bearer " + token);
                }
                super.start(responseListener, headers);
            }
        };
    }
}
