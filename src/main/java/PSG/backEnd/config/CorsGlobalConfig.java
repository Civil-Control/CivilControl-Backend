package PSG.backEnd.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

@Configuration
public class CorsGlobalConfig {

    @Bean
    public FilterRegistrationBean<CorsFilter> corsFilterRegistration() {
        CorsConfiguration cfg = new CorsConfiguration();

        cfg.setAllowedOrigins(List.of(
                "24.232.204.224::4200",
                "http://localhost:8080",
                "http://localhost:4200",
                "http://72.60.15.234:8080",
                "http://72.60.15.234:4200"
        ));
        cfg.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS","HEAD"));
        // enumera explícitamente para evitar 403 por “header no permitido”
        cfg.setAllowedHeaders(List.of("Authorization","Content-Type","Accept","X-Requested-With"));
        cfg.setExposedHeaders(List.of("Location","Content-Disposition"));
        cfg.setAllowCredentials(true);
        cfg.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // registra por si acaso dos patrones (algunos proxys cambian context-path)
        source.registerCorsConfiguration("/**", cfg);
        source.registerCorsConfiguration("/api/**", cfg);
        source.registerCorsConfiguration("/api/v1/**", cfg);

        CorsFilter corsFilter = new CorsFilter(source);
        FilterRegistrationBean<CorsFilter> bean = new FilterRegistrationBean<>(corsFilter);
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return bean;
    }
}
