package app.config;

import app.service.GreenApiSmsService;
import app.service.SmsProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Конфигурация за SMS провайдър
 * 
 * Използва се GREEN-API (WhatsApp) за изпращане на съобщения
 */
@Configuration
@Slf4j
public class SmsProviderConfig {

    @Bean
    @Primary
    public SmsProvider smsProvider(GreenApiSmsService greenApiSmsService) {
        if (greenApiSmsService.isConfigured()) {
            log.info("[SMS Provider] ✓ Using GREEN-API (WhatsApp)");
        } else {
            log.warn("[SMS Provider] ⚠️  GREEN-API not configured - SMS will be logged only");
        }
        return greenApiSmsService;
    }
}

