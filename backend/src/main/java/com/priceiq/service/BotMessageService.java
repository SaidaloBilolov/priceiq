package com.priceiq.service;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class BotMessageService {

    private final Map<String, Map<String, String>> messages = new HashMap<>();

    public BotMessageService() {
        initMessages();
    }

    private void initMessages() {
        // --- Buttons ---
        put("btn.search", "uz", "🔍 Mahsulot Qidirish");
        put("btn.search", "ru", "🔍 Поиск товаров");

        put("btn.top_deals", "uz", "🔥 Eng Yaxshi Takliflar");
        put("btn.top_deals", "ru", "🔥 Лучшие предложения");

        put("btn.favorites", "uz", "⭐ Sevimlilarim");
        put("btn.favorites", "ru", "⭐ Избранное");

        put("btn.alerts", "uz", "🔔 Narx Alertlari");
        put("btn.alerts", "ru", "🔔 Уведомления о цене");

        put("btn.support", "uz", "📞 Qo'llab-quvvatlash");
        put("btn.support", "ru", "📞 Поддержка");

        put("btn.settings", "uz", "⚙️ Sozlamalar");
        put("btn.settings", "ru", "⚙️ Настройки");

        put("btn.open_app", "uz", "🚀 PRICEIQ Ilovasini Ochish");
        put("btn.open_app", "ru", "🚀 Открыть PRICEIQ");

        put("btn.add_product", "uz", "➕ Yangi Mahsulot Qo'shish");
        put("btn.add_product", "ru", "➕ Добавить товар");

        put("btn.my_products", "uz", "📦 Mening Mahsulotlarim");
        put("btn.my_products", "ru", "📦 Мои товары");

        put("btn.update_price", "uz", "✏️ Narxni Yangilash");
        put("btn.update_price", "ru", "✏️ Обновить цену");

        put("btn.select_lang", "uz", "🌐 Tilni tanlash");
        put("btn.select_lang", "ru", "🌐 Выбрать язык");

        put("btn.main_menu", "uz", "🏠 Asosiy Menyu / Suhbatni yakunlash");
        put("btn.main_menu", "ru", "🏠 Главное меню / Завершить");

        // --- Message Responses ---
        put("msg.search_prompt", "uz", "🔍 Mahsulot nomini kiriting (masalan: iPhone 16, Samsung TV):");
        put("msg.search_prompt", "ru", "🔍 Введите название товара (например: iPhone 16, Samsung TV):");

        put("msg.favorites_empty", "uz", "Sizda hali saralangan mahsulotlar yo'q.");
        put("msg.favorites_empty", "ru", "У вас пока нет сохраненных товаров в избранном.");

        put("msg.favorites_header", "uz", "⭐ Sizning Sevimli Mahsulotlaringiz:\n\n");
        put("msg.favorites_header", "ru", "⭐ Ваши избранные товары:\n\n");

        put("msg.alerts_empty", "uz", "🔔 Sizda hozircha narx tushishi xabarnomalari o'rnatilmagan.");
        put("msg.alerts_empty", "ru", "🔔 У вас пока не настроены уведомления о снижении цен.");

        put("msg.alerts_header", "uz", "🔔 Sizning Narx Alertlaringiz:\n\n");
        put("msg.alerts_header", "ru", "🔔 Ваши уведомления о ценах:\n\n");

        put("msg.support_prompt", "uz", "🎧 PRICEIQ Qo'llab-quvvatlash xizmati\n\n✍️ Xabaringizni yozib yuboring:\nMatn, rasm, video yoki ovozli xabar yuborishingiz mumkin.\n\nSupport operatorlarimiz to'g'ridan-to'g'ri ushbu chat orqali javob berishadi!\n\n📞 Ishonch telefoni: +998 71 200 00 00\n👤 Administrator: @priceiq_admin\n\n👇 Xabaringizni yozib yuboring:");
        put("msg.support_prompt", "ru", "🎧 PRICEIQ Служба поддержки\n\n✍️ Напишите ваше сообщение, вопрос или жалобу прямо сюда.\nВы можете отправить текст, фото, видео или голосовое сообщение.\n\nОператоры поддержки ответят вам прямо в этом чате!\n\n📞 Телефон доверия: +998 71 200 00 00\n👤 Администратор: @priceiq_admin\n\n👇 Отправьте ваше сообщение:");

        put("msg.support_confirm", "uz", "✅ Xabaringiz support operatorga yetkazildi!\n\nSuhbatni shu yerda davom ettirishingiz mumkin. Menyuga qaytish uchun '🏠 Asosiy Menyu' tugmasini bosing.");
        put("msg.support_confirm", "ru", "✅ Ваше сообщение передано оператору поддержки!\n\nВы можете продолжать писать прямо сюда. Для выхода нажмите '🏠 Главное меню'.");

        put("msg.lang_set_uz", "uz", "✅ Til muvaffaqiyatli O'zbekchaga o'rnatildi!");
        put("msg.lang_set_uz", "ru", "✅ Til muvaffaqiyatli O'zbekchaga o'rnatildi!");

        put("msg.lang_set_ru", "uz", "✅ Язык успешно установлен на Русский!");
        put("msg.lang_set_ru", "ru", "✅ Язык успешно установлен на Русский!");

        put("msg.default_help", "uz", "Buyruqni tanlash uchun /menu bosing yoki pastdagi menyu tugmalaridan foydalaning.");
        put("msg.default_help", "ru", "Нажмите /menu или воспользуйтесь кнопками меню ниже.");

        put("msg.deals_header", "uz", "🔥 Bugungi Eng Yaxshi Takliflar:\n\n");
        put("msg.deals_header", "ru", "🔥 Лучшие предложения сегодня:\n\n");

        put("msg.deals_empty", "uz", "Hozircha mahsulotlar mavjud emas.");
        put("msg.deals_empty", "ru", "На данный момент товары отсутствуют.");
    }

    private void put(String key, String lang, String val) {
        messages.computeIfAbsent(key, k -> new HashMap<>()).put(lang.toLowerCase(), val);
    }

    public String getMessage(String key, String lang) {
        String l = (lang != null && lang.toLowerCase().startsWith("ru")) ? "ru" : "uz";
        Map<String, String> langMap = messages.get(key);
        if (langMap != null && langMap.containsKey(l)) {
            return langMap.get(l);
        }
        if (langMap != null && langMap.containsKey("uz")) {
            return langMap.get("uz");
        }
        return key;
    }
}
