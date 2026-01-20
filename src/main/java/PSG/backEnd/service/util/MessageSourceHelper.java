package PSG.backEnd.service.util;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

/**
 * Utility to get translated messages from MessageSource
 * Useful for throwing exceptions with translated messages from services
 */
@Component
public class MessageSourceHelper {

    private final MessageSource messageSource;
    private static MessageSource staticMessageSource;

    // Constructor to initialize the static MessageSource
    public MessageSourceHelper(MessageSource messageSource) {
        this.messageSource = messageSource;
        MessageSourceHelper.staticMessageSource = messageSource;
    }

    /**
     * Gets a translated message using the key from messages_es.properties file
     * Instance version (non-static)
     *
     * @param key The message key (e.g., "vehicle.notFound")
     * @param args Optional arguments to interpolate in the message (e.g., the ID)
     * @return The message translated to Spanish
     *
     * @example
     * String message = messageSourceHelper.getMessage("vehicle.notFound", vehicleId);
     * throw new NotFoundException(message);
     */
    public String getMessage(String key, Object... args) {
        return messageSource.getMessage(key, args, LocaleContextHolder.getLocale());
    }

    /**
     * Gets a translated message using the key from messages_es.properties file
     * STATIC version for use in exceptions
     *
     * @param key The message key (e.g., "vehicle.notFound")
     * @param args Optional arguments to interpolate in the message (e.g., the ID)
     * @return The message translated to Spanish
     *
     * @example
     * throw new NotFoundException(MessageSourceHelper.getMessageStatic("vehicle.notFound", vehicleId));
     */
    public static String getMessageStatic(String key, Object... args) {
        if (staticMessageSource == null) {
            return key; // Fallback if not initialized
        }
        return staticMessageSource.getMessage(key, args, LocaleContextHolder.getLocale());
    }

    /**
     * Gets a translated message, or returns a default message if the key is not found
     *
     * @param key The message key
     * @param defaultMessage Default message if the key is not found
     * @param args Optional arguments
     * @return The translated message or the default message
     */
    public String getMessageOrDefault(String key, String defaultMessage, Object... args) {
        try {
            return messageSource.getMessage(key, args, LocaleContextHolder.getLocale());
        } catch (Exception e) {
            return defaultMessage;
        }
    }
}

