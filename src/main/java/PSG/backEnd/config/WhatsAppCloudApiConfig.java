package PSG.backEnd.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

@Configuration
public class WhatsAppCloudApiConfig {

    @Value("${whatsapp.cloud-api.url}")
    private String apiUrl;

    @Value("${whatsapp.cloud-api.access-token}")
    private String accessToken;

    @Bean
    public RestClient whatsAppRestClient(RestClient.Builder builder) {
        return builder
                .baseUrl(apiUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}
