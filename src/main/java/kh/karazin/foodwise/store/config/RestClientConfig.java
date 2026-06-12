package kh.karazin.foodwise.store.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * RestClient beans for inter-service communication.
 */
@Configuration
public class RestClientConfig {

    @Value("${services.surprisebox-service.url:http://surprisebox-service:8084}")
    private String surpriseBoxServiceUrl;

    @Value("${internal.service.secret}")
    private String internalServiceSecret;

    @Bean
    public RestClient surpriseBoxRestClient() {
        return RestClient.builder()
                .baseUrl(surpriseBoxServiceUrl)
                .defaultHeader("X-Internal-Token", internalServiceSecret)
                .build();
    }
}
