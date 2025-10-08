package PSG.backEnd.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAPIConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        Contact contact = new Contact();
        contact.setName("ESEA SA - PSG Backend Team");
        contact.setEmail("admoficinaesea@gmail.com");

        License license = new License()
                .name("Proprietary License")
                .url("https://www.eseaobras.com");

        Info info = new Info()
                .title("PSG Backend API - Vehicle Management System")
                .version("1.0.0")
                .contact(contact)
                .description("RESTful API for managing vehicles, repairs, fuel loads, insurance policies, " +
                        "suppliers, gas stations, payments, and transactional documents for ESEA SA.")
                .license(license);

        return new OpenAPI().info(info);
    }
}
