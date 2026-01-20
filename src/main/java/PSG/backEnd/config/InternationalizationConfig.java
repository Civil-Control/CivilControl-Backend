package PSG.backEnd.config;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Configuration
public class InternationalizationConfig {

    /**
     * MessageSource configuration to load translated messages
     * from the messages_es.properties file
     */
    @Bean
    public MessageSource messageSource() {
        ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();

        // Location of the message files
        messageSource.setBasename("classpath:messages");

        // UTF-8 encoding to support special Spanish characters
        messageSource.setDefaultEncoding(StandardCharsets.UTF_8.name());

        // Default locale: Spanish
        messageSource.setDefaultLocale(new Locale("es"));

        // Message cache in seconds (-1 = infinite cache, useful in production)
        messageSource.setCacheSeconds(3600);

        // If key is not found, return the key itself instead of throwing exception
        messageSource.setUseCodeAsDefaultMessage(true);

        return messageSource;
    }

    /**
     * Resolve the locale from the Accept-Language header of the HTTP request
     * Uses Spanish by default if not specified
     */
    @Bean
    public LocaleResolver localeResolver() {
        AcceptHeaderLocaleResolver localeResolver = new AcceptHeaderLocaleResolver();

        // Default locale: Spanish (Argentina)
        localeResolver.setDefaultLocale(new Locale("es", "AR"));

        return localeResolver;
    }

    /**
     * Configure the validator to use MessageSource with translated messages
     * This allows Bean Validation validations to use messages from messages_es.properties
     */
    @Bean
    public LocalValidatorFactoryBean getValidator(MessageSource messageSource) {
        LocalValidatorFactoryBean bean = new LocalValidatorFactoryBean();
        bean.setValidationMessageSource(messageSource);
        return bean;
    }
}

