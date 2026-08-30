package com.priceiq.service;

import com.priceiq.dto.ProductDto;
import com.priceiq.entity.ProductOffer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.webapp.WebAppInfo;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class TelegramBotService extends TelegramLongPollingBot {

    @Value("${telegram.bot.username}")
    private String botUsername;

    @Value("${telegram.bot.token}")
    private String botToken;

    @Value("${telegram.bot.webapp-url:http://localhost:3000}")
    private String webappUrl;

    private final ProductService productService;
    private final UserService userService;

    public TelegramBotService(ProductService productService, UserService userService) {
        this.productService = productService;
        this.userService = userService;
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (!update.hasMessage() || !update.getMessage().hasText()) {
            return;
        }

        Message message = update.getMessage();
        Long chatId = message.getChatId();
        String text = message.getText().trim();
        org.telegram.telegrambots.meta.api.objects.User tgUser = message.getFrom();

        if (tgUser != null) {
            userService.getOrCreateUser(tgUser.getId(), tgUser.getFirstName(), tgUser.getUserName(), tgUser.getLanguageCode());
        }

        if ("/start".equalsIgnoreCase(text)) {
            handleStartCommand(chatId, tgUser);
        } else {
            handleProductSearch(chatId, text);
        }
    }

    private void handleStartCommand(Long chatId, org.telegram.telegrambots.meta.api.objects.User tgUser) {
        String name = tgUser != null && tgUser.getFirstName() != null ? tgUser.getFirstName() : "Do'stim";
        
        String text = "👋 *Xush kelibsiz, " + name + "!*\n\n" +
                "📱 *PRICEIQ* — O'zbekistondagi eng hamyonbop smartfon narxlarini topuvchi aqlli yordamchingiz.\n\n" +
                "🔍 Narxlarni solishtirish va eng yaxshi takliflarni ko'rish uchun quyidagi tugmani bosing yoki barcha smartfonlarni izlash uchun chatga nomini yozing (masalan: `iPhone 16`):";

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId.toString());
        sendMessage.setText(text);
        sendMessage.setParseMode("Markdown");

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        List<InlineKeyboardButton> row = new ArrayList<>();

        InlineKeyboardButton webAppBtn = new InlineKeyboardButton();
        webAppBtn.setText("🚀 PRICEIQ App'ni Ochish");
        webAppBtn.setWebApp(new WebAppInfo(webappUrl));
        row.add(webAppBtn);

        rows.add(row);
        markup.setKeyboard(rows);
        sendMessage.setReplyMarkup(markup);

        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void handleProductSearch(Long chatId, String query) {
        List<ProductDto> products = productService.searchProducts(query);

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId.toString());
        sendMessage.setParseMode("Markdown");

        if (products.isEmpty()) {
            sendMessage.setText("🔍 *\"" + query + "\"* bo'yicha hech qanday smartfon topilmadi.\n\nIltimos, boshqacharoq qidirib ko'ring (masalan: `iPhone 15 Pro`, `Samsung S24 Ultra`, `Redmi Note 13`).");
        } else {
            ProductDto top = products.get(0);
            String formattedLowest = formatMoney(top.getLowestPriceUzs());
            String formattedAvg = formatMoney(top.getAveragePriceUzs());

            StringBuilder sb = new StringBuilder();
            sb.append("📱 *").append(top.getTitleUz()).append("*\n\n");
            sb.append("📊 *Baholash:* `").append(top.getDealBadgeUz()).append("`\n");
            sb.append("💰 *Eng arzon narx:* `").append(formattedLowest).append(" so'm`\n");
            sb.append("📈 *Bozor o'rtacha narxi:* `").append(formattedAvg).append(" so'm`\n\n");
            sb.append("🛍️ *Do'konlardagi narxlar:*\n");

            if (top.getOffers() != null) {
                int count = 0;
                for (ProductOffer offer : top.getOffers()) {
                    count++;
                    String storeName = offer.getStore() != null ? offer.getStore().getName() : "Do'kon";
                    String badge = count == 1 ? " 🏆 (#1 Eng Arzon)" : "";
                    sb.append("• ").append(storeName).append(": *").append(formatMoney(offer.getPriceUzs())).append(" so'm*").append(badge).append("\n");
                }
            }

            sendMessage.setText(sb.toString());

            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> rows = new ArrayList<>();
            List<InlineKeyboardButton> row = new ArrayList<>();

            InlineKeyboardButton openBtn = new InlineKeyboardButton();
            openBtn.setText("📱 Mini App'da Ko'rish");
            openBtn.setWebApp(new WebAppInfo(webappUrl + "/product/" + top.getId()));
            row.add(openBtn);

            rows.add(row);
            markup.setKeyboard(rows);
            sendMessage.setReplyMarkup(markup);
        }

        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private String formatMoney(Long amount) {
        if (amount == null) return "0";
        NumberFormat nf = NumberFormat.getInstance(new Locale("fr", "FR"));
        return nf.format(amount);
    }
}
