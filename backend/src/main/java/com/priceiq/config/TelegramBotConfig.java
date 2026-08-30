package com.priceiq.config;

import com.priceiq.bot.SellerBotService;
import com.priceiq.bot.SupportBotService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Configuration
public class TelegramBotConfig {

    @Value("${telegram.bot.token:8603794898:AAEPq2YEv7OFBEoSkzYkrhiPe3JCPqcfDko}")
    private String botToken;

    @Value("${telegram.support-bot.token:8603794898:AAEPq2YEv7OFBEoSkzYkrhiPe3JCPqcfDko}")
    private String supportBotToken;

    @Bean
    public TelegramBotsApi telegramBotsApi(SellerBotService sellerBotService, SupportBotService supportBotService) throws TelegramApiException {
        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
        botsApi.registerBot(sellerBotService);
        
        // If a separate Support Bot Token is provided, register Support Bot instance
        if (supportBotToken != null && !supportBotToken.trim().isEmpty() && !supportBotToken.trim().equals(botToken.trim())) {
            botsApi.registerBot(supportBotService);
        }
        return botsApi;
    }
}
