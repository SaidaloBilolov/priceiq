package com.priceiq.bot;

import com.priceiq.dto.ProductDto;
import com.priceiq.entity.*;
import com.priceiq.repository.*;
import com.priceiq.service.ProductService;
import com.priceiq.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.api.objects.webapp.WebAppInfo;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Primary
public class SellerBotService extends TelegramLongPollingBot {

    @Value("${telegram.bot.username:princeiquz_bot}")
    private String botUsername;

    @Value("${telegram.bot.token:8603794898:AAEPq2YEv7OFBEoSkzYkrhiPe3JCPqcfDko}")
    private String botToken;

    @Value("${telegram.bot.webapp-url:https://frontend-three-gamma-ca7l713sls.vercel.app}")
    private String webappUrl;

    private final ProductRepository productRepository;
    private final ProductOfferRepository offerRepository;
    private final PriceHistoryRepository priceHistoryRepository;
    private final CategoryRepository categoryRepository;
    private final StoreRepository storeRepository;
    private final UserRepository userRepository;
    private final FavoriteRepository favoriteRepository;
    private final PriceAlertRepository priceAlertRepository;
    private final UserService userService;
    private final ProductService productService;

    // In-memory sessions by Chat ID
    private final Map<Long, SellerSession> sessions = new ConcurrentHashMap<>();
    private final Map<Long, String> userLanguageMap = new ConcurrentHashMap<>();

    public SellerBotService(ProductRepository productRepository,
                            ProductOfferRepository offerRepository,
                            PriceHistoryRepository priceHistoryRepository,
                            CategoryRepository categoryRepository,
                            StoreRepository storeRepository,
                            UserRepository userRepository,
                            FavoriteRepository favoriteRepository,
                            PriceAlertRepository priceAlertRepository,
                            UserService userService,
                            ProductService productService) {
        this.productRepository = productRepository;
        this.offerRepository = offerRepository;
        this.priceHistoryRepository = priceHistoryRepository;
        this.categoryRepository = categoryRepository;
        this.storeRepository = storeRepository;
        this.userRepository = userRepository;
        this.favoriteRepository = favoriteRepository;
        this.priceAlertRepository = priceAlertRepository;
        this.userService = userService;
        this.productService = productService;
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
        if (update.hasCallbackQuery()) {
            handleCallbackQuery(update.getCallbackQuery());
            return;
        }

        if (!update.hasMessage()) {
            return;
        }

        Message message = update.getMessage();
        Long chatId = message.getChatId();
        org.telegram.telegrambots.meta.api.objects.User tgUser = message.getFrom();

        if (tgUser != null) {
            userService.getOrCreateUser(tgUser.getId(), tgUser.getFirstName(), tgUser.getUserName(), tgUser.getLanguageCode());
        }

        SellerSession session = sessions.computeIfAbsent(chatId, id -> new SellerSession(chatId, tgUser != null ? tgUser.getId() : id));
        String lang = getUserLanguage(chatId, tgUser);

        // 1. Handle Contact sharing
        if (message.hasContact()) {
            handleContactReceived(chatId, message.getContact(), session, tgUser, lang);
            return;
        }

        // 2. Handle Text Commands & Navigation
        if (message.hasText()) {
            String text = message.getText().trim();

            // Language trigger commands
            if ("/language".equalsIgnoreCase(text) || "/til".equalsIgnoreCase(text) || "🌐 Tilni tanlash / Язык".equalsIgnoreCase(text) || "🌐 Tilni o'zgartirish".equalsIgnoreCase(text)) {
                sendLanguageSelection(chatId);
                return;
            }

            // Global Navigation & Cancellation
            if ("/start".equalsIgnoreCase(text) || "/menu".equalsIgnoreCase(text) || "🏠 Asosiy Menyu".equalsIgnoreCase(text) || "/cancel".equalsIgnoreCase(text)) {
                session.setState(SellerState.MAIN_MENU);
                session.clearTempProductData();
                if (session.getStore() != null) {
                    sendSellerMainMenu(chatId, "ru".equals(lang) ? "Главное меню:" : "Asosiy menyu:", session, lang);
                } else {
                    handleStart(chatId, session, tgUser, lang);
                }
                return;
            }

            // --- Seller Actions ---
            if ("➕ Yangi Mahsulot Qo'shish".equalsIgnoreCase(text) || "➕ Добавить товар".equalsIgnoreCase(text)) {
                startAddProductFlow(chatId, session, lang);
                return;
            }

            if ("📦 Mening Mahsulotlarim".equalsIgnoreCase(text) || "📦 Мои товары".equalsIgnoreCase(text)) {
                handleListMyProducts(chatId, session, lang);
                return;
            }

            if ("✏️ Narxni Yangilash".equalsIgnoreCase(text) || "✏️ Обновить цену".equalsIgnoreCase(text)) {
                handleStartPriceUpdate(chatId, session, lang);
                return;
            }

            if ("📞 Qo'llab-quvvatlash".equalsIgnoreCase(text) || "💬 Yordam / Support".equalsIgnoreCase(text) || "/help".equalsIgnoreCase(text) || "/support".equalsIgnoreCase(text)) {
                handleSupport(chatId, lang);
                return;
            }

            // --- Buyer Actions ---
            if ("🔍 Mahsulot Qidirish".equalsIgnoreCase(text) || "🔍 Поиск товаров".equalsIgnoreCase(text)) {
                SendMessage prompt = new SendMessage();
                prompt.setChatId(chatId.toString());
                prompt.setParseMode("Markdown");
                prompt.setText("ru".equals(lang) ?
                        "🔍 Введите название товара (например: `iPhone 16`, `Samsung TV`, `Холодильник`):" :
                        "🔍 Qidirmoqchi bo'lgan mahsulot nomini yozing (masalan: `iPhone 16`, `Samsung TV`, `Muzlatgich`):");
                send(prompt);
                return;
            }

            if ("🔥 Eng Yaxshi Takliflar".equalsIgnoreCase(text) || "🔥 Лучшие предложения".equalsIgnoreCase(text)) {
                handleTopDeals(chatId, lang);
                return;
            }

            if ("⭐ Sevimlilarim".equalsIgnoreCase(text) || "⭐ Избранное".equalsIgnoreCase(text)) {
                handleBuyerFavorites(chatId, tgUser, lang);
                return;
            }

            if ("🔔 Narx Alertlari".equalsIgnoreCase(text) || "🔔 Уведомления о цене".equalsIgnoreCase(text)) {
                handleBuyerAlerts(chatId, tgUser, lang);
                return;
            }

            if ("⚙️ Sozlamalar".equalsIgnoreCase(text) || "⚙️ Настройки".equalsIgnoreCase(text) || "/settings".equalsIgnoreCase(text)) {
                sendSettingsMenu(chatId, tgUser, session, lang);
                return;
            }

            // Route based on FSM state
            switch (session.getState()) {
                case ADD_PRODUCT_PHOTO -> handlePhotoAsTextOrUrl(chatId, text, session, lang);
                case ADD_PRODUCT_NAME -> handleProductNameInput(chatId, text, session, lang);
                case ADD_PRODUCT_PRICE -> handleProductPriceInput(chatId, text, session, lang);
                case ADD_PRODUCT_DESCRIPTION -> handleProductDescriptionInput(chatId, text, session, lang);
                case UPDATE_PRICE_ENTER -> handleProductNewPriceValue(chatId, text, session, lang);
                default -> handleDefaultSearchOrHelp(chatId, text, lang);
            }
            return;
        }

        // 3. Handle Photo Upload for Add Product
        if (message.hasPhoto() && session.getState() == SellerState.ADD_PRODUCT_PHOTO) {
            handleProductPhotoUpload(chatId, message.getPhoto(), session, lang);
        }
    }

    // --- Language Selection & Helpers ---

    private void sendLanguageSelection(Long chatId) {
        SendMessage msg = new SendMessage();
        msg.setChatId(chatId.toString());
        msg.setParseMode("Markdown");
        msg.setText("🌐 *Tilni tanlang / Выберите язык:*");

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        List<InlineKeyboardButton> r = new ArrayList<>();

        InlineKeyboardButton uzBtn = new InlineKeyboardButton();
        uzBtn.setText("🇺🇿 O'zbekcha");
        uzBtn.setCallbackData("set_lang_uz");

        InlineKeyboardButton ruBtn = new InlineKeyboardButton();
        ruBtn.setText("🇷🇺 Русский");
        ruBtn.setCallbackData("set_lang_ru");

        r.add(uzBtn);
        r.add(ruBtn);
        rows.add(r);

        markup.setKeyboard(rows);
        msg.setReplyMarkup(markup);
        send(msg);
    }

    private String getUserLanguage(Long chatId, org.telegram.telegrambots.meta.api.objects.User tgUser) {
        if (userLanguageMap.containsKey(chatId)) {
            return userLanguageMap.get(chatId);
        }
        if (tgUser != null) {
            Optional<com.priceiq.entity.User> userOpt = userRepository.findByTelegramId(tgUser.getId());
            if (userOpt.isPresent() && userOpt.get().getLanguageCode() != null) {
                String l = userOpt.get().getLanguageCode();
                userLanguageMap.put(chatId, l);
                return l;
            }
        }
        return "uz";
    }

    private void setUserLanguage(Long chatId, Long telegramId, String lang) {
        userLanguageMap.put(chatId, lang);
        userRepository.findByTelegramId(telegramId).ifPresent(user -> {
            user.setLanguageCode(lang);
            userRepository.save(user);
        });
    }

    // --- Authentication & Start ---

    private void handleStart(Long chatId, SellerSession session, org.telegram.telegrambots.meta.api.objects.User tgUser, String lang) {
        String name = tgUser != null && tgUser.getFirstName() != null ? tgUser.getFirstName() : "Foydalanuvchi";

        // Check if user is already a registered Store Owner
        Optional<Store> existingStore = storeRepository.findByOwnerChatId(chatId);
        if (existingStore.isPresent()) {
            session.setStore(existingStore.get());
            session.setPhoneNumber(existingStore.get().getOwnerPhone());
            session.setState(SellerState.MAIN_MENU);
            String welcomeMsg = "ru".equals(lang) ?
                    "👋 *Добро пожаловать, " + name + "!*\n\n🏪 Ваш магазин: *" + existingStore.get().getName() + "*\n\nИспользуйте меню ниже для управления товарами:" :
                    "👋 *Xush kelibsiz, " + name + "!*\n\n🏪 Do'koningiz: *" + existingStore.get().getName() + "*\n\nQuyidagi menyu orqali mahsulotlaringizni boshqaring:";
            sendSellerMainMenu(chatId, welcomeMsg, session, lang);
            return;
        }

        // Check if user has already shared their phone number before
        if (tgUser != null) {
            Optional<com.priceiq.entity.User> dbUser = userRepository.findByTelegramId(tgUser.getId());
            if (dbUser.isPresent() && dbUser.get().getPhoneNumber() != null && !dbUser.get().getPhoneNumber().isEmpty()) {
                session.setPhoneNumber(dbUser.get().getPhoneNumber());
                session.setState(SellerState.MAIN_MENU);
                String welcomeBuyer = "ru".equals(lang) ?
                        "👋 *Здравствуйте, " + name + "!*\n\nДобро пожаловать в режим покупателя PRICEIQ. Вы можете искать и сравнивать лучшие цены:" :
                        "👋 *Assalomu alaykum, " + name + "!*\n\nPRICEIQ Xaridor rejimiga xush kelibsiz. Eng qulay narxlarni qidirishingiz va solishtirishingiz mumkin:";
                sendBuyerMainMenu(chatId, welcomeBuyer, tgUser, lang);
                return;
            }
        }

        // Prompt for Contact
        session.setState(SellerState.AWAITING_CONTACT);
        SendMessage msg = new SendMessage();
        msg.setChatId(chatId.toString());
        msg.setParseMode("Markdown");

        if ("ru".equals(lang)) {
            msg.setText("👋 *Здравствуйте, " + name + "!*\n\n" +
                    "🛒 *PRICEIQ* — Умная платформа сравнения цен во всех магазинах Узбекистана.\n\n" +
                    "Чтобы подтвердить магазин (для продавцов) или привязать профиль (для покупателей), нажмите кнопку *📱 Отправить номер телефона* ниже:");
        } else {
            msg.setText("👋 *Assalomu alaykum, " + name + "!*\n\n" +
                    "🛒 *PRICEIQ* — O'zbekistondagi barcha do'konlar narxlarini solishtiruvchi aqlli platforma.\n\n" +
                    "Do'koningizni tasdiqlash (sotuvchilar uchun) yoki shaxsiy profilingizni ulash uchun pastdagi *📱 Telefon Raqamni Yuborish* tugmasini bosing:");
        }

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(true);

        List<KeyboardRow> rows = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        KeyboardButton contactButton = new KeyboardButton("ru".equals(lang) ? "📱 Отправить номер телефона" : "📱 Telefon Raqamni Yuborish");
        contactButton.setRequestContact(true);
        row.add(contactButton);
        rows.add(row);

        keyboardMarkup.setKeyboard(rows);
        msg.setReplyMarkup(keyboardMarkup);

        send(msg);
    }

    private void handleContactReceived(Long chatId, Contact contact, SellerSession session, org.telegram.telegrambots.meta.api.objects.User tgUser, String lang) {
        String phone = contact.getPhoneNumber();
        if (phone == null) phone = "";
        String cleanPhone = phone.replaceAll("[^0-9]", "");

        if (tgUser != null) {
            userService.updatePhoneNumber(tgUser.getId(), phone.startsWith("+") ? phone : "+" + phone, lang);
        }

        Optional<Store> storeOpt = storeRepository.findByCleanPhone(cleanPhone);
        if (storeOpt.isEmpty()) {
            storeOpt = storeRepository.findByOwnerPhone("+" + cleanPhone);
        }
        if (storeOpt.isEmpty()) {
            storeOpt = storeRepository.findByOwnerPhone(phone);
        }

        if (storeOpt.isPresent()) {
            Store store = storeOpt.get();
            store.setOwnerChatId(chatId);
            storeRepository.save(store);

            session.setStore(store);
            session.setPhoneNumber(phone);
            session.setState(SellerState.MAIN_MENU);

            String text = "ru".equals(lang) ?
                    "✅ *Поздравляем, ваш магазин успешно подключен!*\n\n🏪 *Магазин:* `" + store.getName() + "`\n📞 *Телефон:* `" + phone + "`\n\nИспользуйте меню ниже для добавления товаров и управления ценами:" :
                    "✅ *Tabriklaymiz, Do'koningiz Muvaffaqiyatli Ulandi!*\n\n🏪 *Do'kon:* `" + store.getName() + "`\n📞 *Telefon:* `" + phone + "`\n\nEndi quyidagi menyu orqali yangi mahsulot qo'shishingiz va narxlarni boshqarishingiz mumkin:";
            sendSellerMainMenu(chatId, text, session, lang);
        } else {
            session.setStore(null);
            session.setPhoneNumber(phone);
            session.setState(SellerState.MAIN_MENU);

            String text = "ru".equals(lang) ?
                    "✅ *Ваш номер телефона успешно сохранен!*\n\nВы находитесь в режиме *Покупателя*. Вы можете искать самые низкие цены и следить за скидками:" :
                    "✅ *Telefon raqamingiz muvaffaqiyatli saqlandi!*\n\nSiz *PRICEIQ Xaridor* rejimidasiz. Barcha do'konlardagi eng arzon narxlarni qidirishingiz va narx tushishini kuzatishingiz mumkin:";
            sendBuyerMainMenu(chatId, text, tgUser, lang);
        }
    }

    // --- Main Menus (Seller vs Buyer) ---

    private void sendSellerMainMenu(Long chatId, String text, SellerSession session, String lang) {
        SendMessage msg = new SendMessage();
        msg.setChatId(chatId.toString());
        msg.setText(text);
        msg.setParseMode("Markdown");

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);

        List<KeyboardRow> rows = new ArrayList<>();

        KeyboardRow r1 = new KeyboardRow();
        r1.add(new KeyboardButton("ru".equals(lang) ? "➕ Добавить товар" : "➕ Yangi Mahsulot Qo'shish"));
        r1.add(new KeyboardButton("ru".equals(lang) ? "📦 Мои товары" : "📦 Mening Mahsulotlarim"));

        KeyboardRow r2 = new KeyboardRow();
        r2.add(new KeyboardButton("ru".equals(lang) ? "✏️ Обновить цену" : "✏️ Narxni Yangilash"));
        r2.add(new KeyboardButton("🌐 Tilni tanlash / Язык"));

        KeyboardRow r3 = new KeyboardRow();
        r3.add(new KeyboardButton("ru".equals(lang) ? "⚙️ Настройки" : "⚙️ Sozlamalar"));
        r3.add(new KeyboardButton("ru".equals(lang) ? "📞 Поддержка" : "📞 Qo'llab-quvvatlash"));

        KeyboardRow r4 = new KeyboardRow();
        KeyboardButton appBtn = new KeyboardButton("ru".equals(lang) ? "🚀 Открыть PRICEIQ" : "🚀 PRICEIQ Ilovasini Ochish");
        appBtn.setWebApp(new WebAppInfo(webappUrl));
        r4.add(appBtn);

        rows.add(r1);
        rows.add(r2);
        rows.add(r3);
        rows.add(r4);
        keyboardMarkup.setKeyboard(rows);
        msg.setReplyMarkup(keyboardMarkup);

        send(msg);
    }

    private void sendBuyerMainMenu(Long chatId, String text, org.telegram.telegrambots.meta.api.objects.User tgUser, String lang) {
        SendMessage msg = new SendMessage();
        msg.setChatId(chatId.toString());
        msg.setText(text);
        msg.setParseMode("Markdown");

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);

        List<KeyboardRow> rows = new ArrayList<>();

        KeyboardRow r1 = new KeyboardRow();
        r1.add(new KeyboardButton("ru".equals(lang) ? "🔍 Поиск товаров" : "🔍 Mahsulot Qidirish"));
        r1.add(new KeyboardButton("ru".equals(lang) ? "🔥 Лучшие предложения" : "🔥 Eng Yaxshi Takliflar"));

        KeyboardRow r2 = new KeyboardRow();
        r2.add(new KeyboardButton("ru".equals(lang) ? "⭐ Избранное" : "⭐ Sevimlilarim"));
        r2.add(new KeyboardButton("ru".equals(lang) ? "🔔 Уведомления о цене" : "🔔 Narx Alertlari"));

        KeyboardRow r3 = new KeyboardRow();
        r3.add(new KeyboardButton("🌐 Tilni tanlash / Язык"));
        r3.add(new KeyboardButton("ru".equals(lang) ? "⚙️ Настройки" : "⚙️ Sozlamalar"));

        KeyboardRow r4 = new KeyboardRow();
        KeyboardButton appBtn = new KeyboardButton("ru".equals(lang) ? "🚀 Открыть PRICEIQ" : "🚀 PRICEIQ Ilovasini Ochish");
        appBtn.setWebApp(new WebAppInfo(webappUrl));
        r4.add(appBtn);

        rows.add(r1);
        rows.add(r2);
        rows.add(r3);
        rows.add(r4);
        keyboardMarkup.setKeyboard(rows);
        msg.setReplyMarkup(keyboardMarkup);

        send(msg);
    }

    // --- Settings Menu & Sub-Options ---

    private void sendSettingsMenu(Long chatId, org.telegram.telegrambots.meta.api.objects.User tgUser, SellerSession session, String lang) {
        SendMessage msg = new SendMessage();
        msg.setChatId(chatId.toString());
        msg.setParseMode("Markdown");
        msg.setText("ru".equals(lang) ?
                "⚙️ *Раздел настроек:*\n\nВыберите нужное действие:" :
                "⚙️ *Sozlamalar bo'limi:*\n\nQuyidagi amallardan birini tanlang:");

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        List<InlineKeyboardButton> r1 = new ArrayList<>();
        InlineKeyboardButton langBtn = new InlineKeyboardButton();
        langBtn.setText("🌐 Til / Язык (UZ / RU)");
        langBtn.setCallbackData("settings_lang");
        r1.add(langBtn);

        List<InlineKeyboardButton> r2 = new ArrayList<>();
        InlineKeyboardButton profileBtn = new InlineKeyboardButton();
        profileBtn.setText("ru".equals(lang) ? "👤 Мой профиль" : "👤 Mening profilim");
        profileBtn.setCallbackData("settings_profile");
        r2.add(profileBtn);

        List<InlineKeyboardButton> r3 = new ArrayList<>();
        InlineKeyboardButton switchBtn = new InlineKeyboardButton();
        switchBtn.setText("ru".equals(lang) ? "🔄 Переключить режим (Продавец/Покупатель)" : "🔄 Rejimni almashtirish (Sotuvchi/Xaridor)");
        switchBtn.setCallbackData("settings_switch_seller");
        r3.add(switchBtn);

        List<InlineKeyboardButton> r4 = new ArrayList<>();
        InlineKeyboardButton backBtn = new InlineKeyboardButton();
        backBtn.setText("ru".equals(lang) ? "🔙 В главное меню" : "🔙 Bosh menyuga qaytish");
        backBtn.setCallbackData("settings_back");
        r4.add(backBtn);

        rows.add(r1);
        rows.add(r2);
        rows.add(r3);
        rows.add(r4);
        markup.setKeyboard(rows);
        msg.setReplyMarkup(markup);

        send(msg);
    }

    // --- Simplified Add Product FSM Flow ---

    private void startAddProductFlow(Long chatId, SellerSession session, String lang) {
        if (session.getStore() == null) {
            handleStart(chatId, session, null, lang);
            return;
        }

        session.clearTempProductData();
        session.setState(SellerState.ADD_PRODUCT_PHOTO);

        SendMessage msg = new SendMessage();
        msg.setChatId(chatId.toString());
        msg.setParseMode("Markdown");
        msg.setText("ru".equals(lang) ?
                "📸 *Шаг 1: Отправьте фото товара.*\n\nОтправьте фото, ссылку на изображение или нажмите 'Пропустить':" :
                "📸 *1-qadam: Mahsulot rasmini yuboring.*\n\nRasm faylini yuboring yoki rasm havolasini (URL) yozing (yoki 'O'tkazib yuborish' deb yozing):");

        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        keyboard.setResizeKeyboard(true);
        List<KeyboardRow> rows = new ArrayList<>();
        KeyboardRow r = new KeyboardRow();
        r.add(new KeyboardButton("ru".equals(lang) ? "⏩ Пропустить" : "⏩ O'tkazib yuborish"));
        r.add(new KeyboardButton("ru".equals(lang) ? "🏠 Главное меню" : "🏠 Asosiy Menyu"));
        rows.add(r);
        keyboard.setKeyboard(rows);
        msg.setReplyMarkup(keyboard);

        send(msg);
    }

    private void handlePhotoAsTextOrUrl(Long chatId, String text, SellerSession session, String lang) {
        if (text.startsWith("http://") || text.startsWith("https://")) {
            session.setTempPhotoUrl(text);
        } else {
            session.setTempPhotoUrl("https://images.unsplash.com/photo-1523275335684-37898b6baf30?auto=format&fit=crop&w=600&q=80");
        }

        session.setState(SellerState.ADD_PRODUCT_NAME);

        SendMessage msg = new SendMessage();
        msg.setChatId(chatId.toString());
        msg.setParseMode("Markdown");
        msg.setText("ru".equals(lang) ?
                "📝 *Шаг 2: Введите название товара.*\n\nПример: `iPhone 16 Pro Max 256GB` или `Телевизор Samsung 55\"`:" :
                "📝 *2-qadam: Mahsulot nomini kiriting.*\n\nMisol: `iPhone 16 Pro Max 256GB` yoki `Artel Kir yuvish mashinasi`:");

        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        keyboard.setResizeKeyboard(true);
        List<KeyboardRow> rows = new ArrayList<>();
        KeyboardRow r = new KeyboardRow();
        r.add(new KeyboardButton("ru".equals(lang) ? "🏠 Главное меню" : "🏠 Asosiy Menyu"));
        rows.add(r);
        keyboard.setKeyboard(rows);
        msg.setReplyMarkup(keyboard);

        send(msg);
    }

    private void handleProductPhotoUpload(Long chatId, List<PhotoSize> photos, SellerSession session, String lang) {
        PhotoSize largestPhoto = photos.stream()
                .max(Comparator.comparing(PhotoSize::getFileSize))
                .orElse(photos.get(photos.size() - 1));

        String fileId = largestPhoto.getFileId();
        session.setTempPhotoFileId(fileId);

        try {
            GetFile getFileMethod = new GetFile(fileId);
            org.telegram.telegrambots.meta.api.objects.File tgFile = execute(getFileMethod);
            String photoUrl = "https://api.telegram.org/file/bot" + botToken + "/" + tgFile.getFilePath();
            session.setTempPhotoUrl(photoUrl);
        } catch (Exception e) {
            session.setTempPhotoUrl("https://images.unsplash.com/photo-1523275335684-37898b6baf30?auto=format&fit=crop&w=600&q=80");
        }

        session.setState(SellerState.ADD_PRODUCT_NAME);

        SendMessage msg = new SendMessage();
        msg.setChatId(chatId.toString());
        msg.setParseMode("Markdown");
        msg.setText("ru".equals(lang) ?
                "✅ Фото принято!\n\n📝 *Шаг 2: Введите название товара.*\n\nПример: `iPhone 16 Pro Max 256GB` или `Samsung TV 55\"`:" :
                "✅ Rasm qabul qilindi!\n\n📝 *2-qadam: Mahsulot nomini kiriting.*\n\nMisol: `iPhone 16 Pro Max 256GB` yoki `Samsung TV 55\"`:");
        send(msg);
    }

    private void handleProductNameInput(Long chatId, String text, SellerSession session, String lang) {
        session.setTempTitle(text);
        session.setState(SellerState.ADD_PRODUCT_PRICE);

        SendMessage msg = new SendMessage();
        msg.setChatId(chatId.toString());
        msg.setParseMode("Markdown");
        msg.setText("ru".equals(lang) ?
                "💰 *Шаг 3: Введите цену товара.*\n\nПример: `15 000 000` или `1200 USD` или `8500000`:" :
                "💰 *3-qadam: Mahsulot narxini kiriting.*\n\nMisol: `15 000 000` yoki `1200 USD` yoki `8500000`:");
        send(msg);
    }

    private void handleProductPriceInput(Long chatId, String text, SellerSession session, String lang) {
        Long priceUzs = parsePrice(text);
        if (priceUzs == null || priceUzs <= 0) {
            SendMessage msg = new SendMessage();
            msg.setChatId(chatId.toString());
            msg.setText("ru".equals(lang) ?
                    "❌ Неверная цена. Введите только цифры (например: `12500000` или `1000 USD`):" :
                    "❌ Narx noto'g'ri kiritildi. Faqat son kiriting (masalan: `12500000` yoki `1000 USD`):");
            send(msg);
            return;
        }

        session.setTempPriceUzs(priceUzs);
        session.setState(SellerState.ADD_PRODUCT_DESCRIPTION);

        SendMessage msg = new SendMessage();
        msg.setChatId(chatId.toString());
        msg.setParseMode("Markdown");
        msg.setText("ru".equals(lang) ?
                "📄 *Шаг 4: Введите краткое описание / комментарий к товару.*\n\n(Гарантия, характеристики или нажмите 'Пропустить'):" :
                "📄 *4-qadam: Mahsulot haqida qisqacha tavsif / izoh kiriting.*\n\n(Kafolat, xususiyatlar yoki 'O'tkazib yuborish' tugmasini bosing):");

        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        keyboard.setResizeKeyboard(true);
        List<KeyboardRow> rows = new ArrayList<>();
        KeyboardRow r = new KeyboardRow();
        r.add(new KeyboardButton("ru".equals(lang) ? "⏩ Пропустить" : "⏩ O'tkazib yuborish"));
        r.add(new KeyboardButton("ru".equals(lang) ? "🏠 Главное меню" : "🏠 Asosiy Menyu"));
        rows.add(r);
        keyboard.setKeyboard(rows);
        msg.setReplyMarkup(keyboard);

        send(msg);
    }

    private void handleProductDescriptionInput(Long chatId, String text, SellerSession session, String lang) {
        if ("⏩ O'tkazib yuborish".equalsIgnoreCase(text) || "⏩ Пропустить".equalsIgnoreCase(text) || "O'tkazib yuborish".equalsIgnoreCase(text) || "Yo'q".equalsIgnoreCase(text)) {
            session.setTempDescription("ru".equals(lang) ? "Новый товар. Официальная гарантия." : "Yangi mahsulot. Rasmiy kafolat bilan.");
        } else {
            session.setTempDescription(text);
        }

        session.setState(SellerState.ADD_PRODUCT_CONFIRM);

        String caption = "ru".equals(lang) ?
                "📋 *Шаг 5: Подтвердите данные товара:*\n\n" +
                        "🏷️ *Название:* `" + session.getTempTitle() + "`\n" +
                        "🏪 *Магазин:* `" + session.getStore().getName() + "`\n" +
                        "💰 *Цена:* `" + formatMoney(session.getTempPriceUzs()) + " сум`\n" +
                        "📄 *Описание:* " + session.getTempDescription() + "\n\n" +
                        "Все данные верны?" :
                "📋 *5-qadam: Mahsulot ma'lumotlarini tasdiqlang:*\n\n" +
                        "🏷️ *Nomi:* `" + session.getTempTitle() + "`\n" +
                        "🏪 *Do'kon:* `" + session.getStore().getName() + "`\n" +
                        "💰 *Narxi:* `" + formatMoney(session.getTempPriceUzs()) + " so'm`\n" +
                        "📄 *Tavsif:* " + session.getTempDescription() + "\n\n" +
                        "Barcha ma'lumotlar to'g'rimi?";

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        List<InlineKeyboardButton> r = new ArrayList<>();

        InlineKeyboardButton confirmBtn = new InlineKeyboardButton();
        confirmBtn.setText("ru".equals(lang) ? "✅ Подтвердить и Сохранить" : "✅ Tasdiqlash va Saqlash");
        confirmBtn.setCallbackData("confirm_add_product");

        InlineKeyboardButton cancelBtn = new InlineKeyboardButton();
        cancelBtn.setText("ru".equals(lang) ? "❌ Отменить" : "❌ Bekor qilish");
        cancelBtn.setCallbackData("cancel_add_product");

        r.add(confirmBtn);
        r.add(cancelBtn);
        rows.add(r);
        markup.setKeyboard(rows);

        if (session.getTempPhotoFileId() != null) {
            SendPhoto sendPhoto = new SendPhoto();
            sendPhoto.setChatId(chatId.toString());
            sendPhoto.setPhoto(new InputFile(session.getTempPhotoFileId()));
            sendPhoto.setCaption(caption);
            sendPhoto.setParseMode("Markdown");
            sendPhoto.setReplyMarkup(markup);
            try {
                execute(sendPhoto);
            } catch (TelegramApiException e) {
                sendFallbackPreview(chatId, caption, markup);
            }
        } else {
            sendFallbackPreview(chatId, caption, markup);
        }
    }

    private void sendFallbackPreview(Long chatId, String caption, InlineKeyboardMarkup markup) {
        SendMessage msg = new SendMessage();
        msg.setChatId(chatId.toString());
        msg.setText(caption);
        msg.setParseMode("Markdown");
        msg.setReplyMarkup(markup);
        send(msg);
    }

    // --- Callback Query Handling ---

    private void handleCallbackQuery(CallbackQuery query) {
        String data = query.getData();
        Long chatId = query.getMessage().getChatId();
        Integer messageId = query.getMessage().getMessageId();
        org.telegram.telegrambots.meta.api.objects.User tgUser = query.getFrom();
        SellerSession session = sessions.computeIfAbsent(chatId, id -> new SellerSession(chatId, tgUser.getId()));
        String lang = getUserLanguage(chatId, tgUser);

        if ("set_lang_uz".equals(data)) {
            setUserLanguage(chatId, tgUser.getId(), "uz");
            SendMessage msg = new SendMessage();
            msg.setChatId(chatId.toString());
            msg.setText("✅ Til muvaffaqiyatli *O'zbekcha*ga o'rnatildi!");
            msg.setParseMode("Markdown");
            send(msg);
            if (session.getStore() != null) {
                sendSellerMainMenu(chatId, "Asosiy menyu:", session, "uz");
            } else {
                sendBuyerMainMenu(chatId, "Asosiy menyu:", tgUser, "uz");
            }
        } else if ("set_lang_ru".equals(data)) {
            setUserLanguage(chatId, tgUser.getId(), "ru");
            SendMessage msg = new SendMessage();
            msg.setChatId(chatId.toString());
            msg.setText("✅ Язык успешно установлен на *Русский*!");
            msg.setParseMode("Markdown");
            send(msg);
            if (session.getStore() != null) {
                sendSellerMainMenu(chatId, "Главное меню:", session, "ru");
            } else {
                sendBuyerMainMenu(chatId, "Главное меню:", tgUser, "ru");
            }
        } else if ("confirm_add_product".equals(data)) {
            saveNewProductToDatabase(chatId, session, messageId, lang);
        } else if ("cancel_add_product".equals(data)) {
            session.clearTempProductData();
            session.setState(SellerState.MAIN_MENU);
            sendSellerMainMenu(chatId, "ru".equals(lang) ? "❌ Добавление товара отменено." : "❌ Mahsulot qo'shish bekor qilindi.", session, lang);
        } else if (data.startsWith("update_price_")) {
            Long productId = Long.parseLong(data.replace("update_price_", ""));
            session.setTempSelectedProductId(productId);
            session.setState(SellerState.UPDATE_PRICE_ENTER);

            SendMessage msg = new SendMessage();
            msg.setChatId(chatId.toString());
            msg.setParseMode("Markdown");
            msg.setText("ru".equals(lang) ?
                    "💰 Введите *новую цену* для выбранного товара (в сумах):" :
                    "💰 Tanlangan mahsulot uchun *yangi narxni* kiriting (so'mda):");
            send(msg);
        } else if ("settings_lang".equals(data)) {
            sendLanguageSelection(chatId);
        } else if ("settings_profile".equals(data)) {
            String name = tgUser.getFirstName() != null ? tgUser.getFirstName() : "Foydalanuvchi";
            String username = tgUser.getUserName() != null ? "@" + tgUser.getUserName() : "kiritilmagan";
            String phone = session.getPhoneNumber() != null ? session.getPhoneNumber() : "kiritilmagan";
            String role = session.getStore() != null ?
                    ("ru".equals(lang) ? "🏪 Продавец магазина (" + session.getStore().getName() + ")" : "🏪 Do'kon Sotuvchisi (" + session.getStore().getName() + ")") :
                    ("ru".equals(lang) ? "🛍️ Покупатель (Buyer)" : "🛍️ Oddiy Xaridor (Buyer)");

            SendMessage msg = new SendMessage();
            msg.setChatId(chatId.toString());
            msg.setParseMode("Markdown");
            msg.setText("ru".equals(lang) ?
                    "👤 *Профиль пользователя:*\n\n" +
                            "🏷️ *Имя:* `" + name + "`\n" +
                            "🆔 *Telegram ID:* `" + tgUser.getId() + "`\n" +
                            "🔗 *Username:* " + username + "\n" +
                            "📞 *Телефон:* `" + phone + "`\n" +
                            "🎭 *Роль:* " + role :
                    "👤 *Foydalanuvchi Profili:*\n\n" +
                            "🏷️ *Ism:* `" + name + "`\n" +
                            "🆔 *Telegram ID:* `" + tgUser.getId() + "`\n" +
                            "🔗 *Username:* " + username + "\n" +
                            "📞 *Telefon:* `" + phone + "`\n" +
                            "🎭 *Rol:* " + role);
            send(msg);
        } else if ("settings_switch_seller".equals(data)) {
            if (session.getStore() != null) {
                // Currently in Seller mode -> switch to Buyer mode
                session.setStore(null);
                session.setState(SellerState.MAIN_MENU);
                String txt = "ru".equals(lang) ? "✅ Вы успешно переключились в режим *Покупателя*." : "✅ Siz muvaffaqiyatli *Xaridor* rejimiga o'tdingiz.";
                sendBuyerMainMenu(chatId, txt, tgUser, lang);
                return;
            }

            // Check if user is linked to any store
            Optional<Store> storeOpt = storeRepository.findByOwnerChatId(chatId);
            if (storeOpt.isEmpty() && session.getPhoneNumber() != null) {
                String clean = session.getPhoneNumber().replaceAll("[^0-9]", "");
                storeOpt = storeRepository.findByCleanPhone(clean);
            }

            if (storeOpt.isPresent()) {
                session.setStore(storeOpt.get());
                session.setState(SellerState.MAIN_MENU);
                String txt = "ru".equals(lang) ?
                        "✅ *Вы успешно переключились в режим продавца!*\n\n🏪 Магазин: *" + storeOpt.get().getName() + "*" :
                        "✅ *Sotuvchi rejimiga muvaffaqiyatli o'tildi!*\n\n🏪 Do'kon: *" + storeOpt.get().getName() + "*";
                sendSellerMainMenu(chatId, txt, session, lang);
            } else {
                SendMessage msg = new SendMessage();
                msg.setChatId(chatId.toString());
                msg.setParseMode("Markdown");
                msg.setText("ru".equals(lang) ?
                        "⚠️ *Магазин не найден.*\n\nЧтобы стать продавцом, ваш номер телефона должен быть привязан к магазину в панели администратора.\n\nСвяжитесь с администратором: @priceiq_admin" :
                        "⚠️ *Do'kon topilmadi.*\n\nSotuvchi rejimiga o'tish uchun telefon raqamingiz Admin Panelda do'konga biriktirilgan bo'lishi kerak.\n\nIltimos, administrator bilan bog'laning: @priceiq_admin");
                send(msg);
            }
        } else if ("settings_back".equals(data)) {
            if (session.getStore() != null) {
                sendSellerMainMenu(chatId, "ru".equals(lang) ? "Главное меню:" : "Asosiy menyu:", session, lang);
            } else {
                sendBuyerMainMenu(chatId, "ru".equals(lang) ? "Главное меню:" : "Asosiy menyu:", tgUser, lang);
            }
        }
    }

    private void saveNewProductToDatabase(Long chatId, SellerSession session, Integer messageId, String lang) {
        if (session.getStore() == null || session.getTempTitle() == null || session.getTempPriceUzs() == null) {
            SendMessage msg = new SendMessage();
            msg.setChatId(chatId.toString());
            msg.setText("ru".equals(lang) ? "⚠️ Данные сессии не найдены. Попробуйте снова." : "⚠️ Sessiya ma'lumotlari topilmadi. Qaytadan urinib ko'ring.");
            send(msg);
            return;
        }

        try {
            Category defaultCat = categoryRepository.findAll().stream().findFirst()
                    .orElseGet(() -> categoryRepository.save(new Category(null, "Boshqa Mahsulotlar", "Другие товары", "Other", "Box")));

            String imageUrl = session.getTempPhotoUrl() != null ? session.getTempPhotoUrl() :
                    "https://images.unsplash.com/photo-1523275335684-37898b6baf30?auto=format&fit=crop&w=600&q=80";

            Product product = new Product(
                    null,
                    session.getTempTitle(),
                    session.getTempTitle(),
                    session.getTempTitle(),
                    "Universal",
                    "",
                    "",
                    "",
                    "",
                    imageUrl,
                    defaultCat
            );
            product.setDescriptionUz(session.getTempDescription());
            product.setDescriptionRu(session.getTempDescription());
            product = productRepository.save(product);

            ProductOffer offer = new ProductOffer(
                    null,
                    product,
                    session.getStore(),
                    session.getTempPriceUzs(),
                    (long) (session.getTempPriceUzs() * 1.05),
                    true,
                    session.getStore().getWebsiteUrl() != null ? session.getStore().getWebsiteUrl() : "https://uzum.uz"
            );
            offerRepository.save(offer);

            priceHistoryRepository.save(new PriceHistory(null, product, session.getTempPriceUzs(), LocalDateTime.now()));

            session.clearTempProductData();
            session.setState(SellerState.MAIN_MENU);

            SendMessage successMsg = new SendMessage();
            successMsg.setChatId(chatId.toString());
            successMsg.setParseMode("Markdown");
            successMsg.setText("ru".equals(lang) ?
                    "🎉 *Поздравляем!*\n\n" +
                            "✅ Товар успешно сохранен и мгновенно опубликован в Web и Mini App!\n\n" +
                            "🆔 *ID товара:* `" + product.getId() + "`\n" +
                            "🏷️ *Название:* `" + product.getTitleUz() + "`\n" +
                            "💰 *Цена:* `" + formatMoney(offer.getPriceUzs()) + " сум`" :
                    "🎉 *Tabriklaymiz!*\n\n" +
                            "✅ Mahsulot muvaffaqiyatli saqlandi va barcha tizimlarda (Web, Mini App) darhol e'lon qilindi!\n\n" +
                            "🆔 *Mahsulot ID:* `" + product.getId() + "`\n" +
                            "🏷️ *Nomi:* `" + product.getTitleUz() + "`\n" +
                            "💰 *Narxi:* `" + formatMoney(offer.getPriceUzs()) + " so'm`");

            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> rows = new ArrayList<>();
            List<InlineKeyboardButton> r = new ArrayList<>();

            InlineKeyboardButton viewBtn = new InlineKeyboardButton();
            viewBtn.setText("ru".equals(lang) ? "📱 Посмотреть в приложении" : "📱 Mini App'da Ko'rish");
            viewBtn.setWebApp(new WebAppInfo(webappUrl + "/product/" + product.getId()));
            r.add(viewBtn);
            rows.add(r);
            markup.setKeyboard(rows);
            successMsg.setReplyMarkup(markup);

            send(successMsg);
            sendSellerMainMenu(chatId, "ru".equals(lang) ? "Главное меню:" : "Asosiy menyu:", session, lang);

        } catch (Exception e) {
            e.printStackTrace();
            SendMessage errMsg = new SendMessage();
            errMsg.setChatId(chatId.toString());
            errMsg.setText("❌ Error: " + e.getMessage());
            send(errMsg);
        }
    }

    // --- Product List & Price Update ---

    private void handleListMyProducts(Long chatId, SellerSession session, String lang) {
        if (session.getStore() == null) {
            handleStart(chatId, session, null, lang);
            return;
        }

        List<ProductOffer> myOffers = offerRepository.findByStoreId(session.getStore().getId());
        if (myOffers.isEmpty()) {
            SendMessage msg = new SendMessage();
            msg.setChatId(chatId.toString());
            msg.setParseMode("Markdown");
            msg.setText("ru".equals(lang) ?
                    "📦 *В вашем магазине пока нет товаров.*\n\nЧтобы добавить новый товар, нажмите *➕ Добавить товар*." :
                    "📦 *Do'koningizda hali mahsulotlar mavjud emas.*\n\nYangi mahsulot qo'shish uchun *➕ Yangi Mahsulot Qo'shish* tugmasini bosing.");
            send(msg);
            return;
        }

        StringBuilder sb = new StringBuilder();
        if ("ru".equals(lang)) {
            sb.append("🏪 Товары магазина *").append(session.getStore().getName()).append("* (Всего: ").append(myOffers.size()).append(" шт.):\n\n");
        } else {
            sb.append("🏪 *").append(session.getStore().getName()).append("* mahsulotlari (Jami: ").append(myOffers.size()).append(" ta):\n\n");
        }

        int count = 0;
        for (ProductOffer offer : myOffers) {
            count++;
            sb.append(count).append(". *").append(offer.getProduct().getTitleUz()).append("*\n")
                    .append("   💰 ").append("ru".equals(lang) ? "Цена: `" : "Narxi: `").append(formatMoney(offer.getPriceUzs())).append(" so'm`\n\n");
            if (count >= 10) break;
        }

        SendMessage msg = new SendMessage();
        msg.setChatId(chatId.toString());
        msg.setParseMode("Markdown");
        msg.setText(sb.toString());
        send(msg);
    }

    private void handleStartPriceUpdate(Long chatId, SellerSession session, String lang) {
        if (session.getStore() == null) {
            handleStart(chatId, session, null, lang);
            return;
        }

        List<ProductOffer> myOffers = offerRepository.findByStoreId(session.getStore().getId());
        if (myOffers.isEmpty()) {
            SendMessage msg = new SendMessage();
            msg.setChatId(chatId.toString());
            msg.setText("ru".equals(lang) ? "📦 Нет товаров для обновления цен." : "📦 Narxini o'zgartirish uchun mahsulotlar topilmadi.");
            send(msg);
            return;
        }

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        for (ProductOffer offer : myOffers) {
            List<InlineKeyboardButton> r = new ArrayList<>();
            InlineKeyboardButton btn = new InlineKeyboardButton();
            btn.setText("✏️ " + offer.getProduct().getTitleUz() + " (" + formatMoney(offer.getPriceUzs()) + " so'm)");
            btn.setCallbackData("update_price_" + offer.getProduct().getId());
            r.add(btn);
            rows.add(r);
            if (rows.size() >= 8) break;
        }

        markup.setKeyboard(rows);

        SendMessage msg = new SendMessage();
        msg.setChatId(chatId.toString());
        msg.setParseMode("Markdown");
        msg.setText("ru".equals(lang) ? "✏️ Выберите товар для обновления цены:" : "✏️ Narxini yangilamoqchi bo'lgan mahsulotni tanlang:");
        msg.setReplyMarkup(markup);
        send(msg);
    }

    private void handleProductNewPriceValue(Long chatId, String text, SellerSession session, String lang) {
        Long newPrice = parsePrice(text);
        if (newPrice == null || newPrice <= 0) {
            SendMessage msg = new SendMessage();
            msg.setChatId(chatId.toString());
            msg.setText("ru".equals(lang) ? "❌ Неверная цена. Введите только цифры:" : "❌ Noto'g'ri narx. Faqat son kiriting:");
            send(msg);
            return;
        }

        Long prodId = session.getTempSelectedProductId();
        if (prodId != null && session.getStore() != null) {
            List<ProductOffer> offers = offerRepository.findByProductId(prodId);
            for (ProductOffer offer : offers) {
                if (offer.getStore().getId().equals(session.getStore().getId())) {
                    offer.setOldPriceUzs(offer.getPriceUzs());
                    offer.setPriceUzs(newPrice);
                    offerRepository.save(offer);

                    productRepository.findById(prodId).ifPresent(p -> {
                        priceHistoryRepository.save(new PriceHistory(null, p, newPrice, LocalDateTime.now()));
                    });

                    SendMessage success = new SendMessage();
                    success.setChatId(chatId.toString());
                    success.setParseMode("Markdown");
                    success.setText("ru".equals(lang) ?
                            "✅ *Цена успешно обновлена!*\n\n💰 Новая цена: `" + formatMoney(newPrice) + " сум`" :
                            "✅ *Narx muvaffaqiyatli yangilandi!*\n\n💰 Yangi narx: `" + formatMoney(newPrice) + " so'm`");
                    send(success);
                    break;
                }
            }
        }

        session.clearTempProductData();
        session.setState(SellerState.MAIN_MENU);
        sendSellerMainMenu(chatId, "ru".equals(lang) ? "Главное меню:" : "Asosiy menyu:", session, lang);
    }

    // --- Buyer Features ---

    private void handleTopDeals(Long chatId, String lang) {
        List<ProductDto> all = productService.getAllProducts();
        if (all.isEmpty()) {
            SendMessage msg = new SendMessage();
            msg.setChatId(chatId.toString());
            msg.setText("ru".equals(lang) ? "На данный момент товары отсутствуют." : "Hozircha mahsulotlar mavjud emas.");
            send(msg);
            return;
        }

        StringBuilder sb = new StringBuilder();
        if ("ru".equals(lang)) {
            sb.append("🔥 *Лучшие предложения сегодня:*\n\n");
        } else {
            sb.append("🔥 *Bugungi Eng Yaxshi Takliflar:*\n\n");
        }

        int count = 0;
        for (ProductDto p : all) {
            count++;
            sb.append(count).append(". 📱 *").append(p.getTitleUz()).append("*\n")
                    .append("   💰 ").append("ru".equals(lang) ? "Цена: `" : "Narxi: `").append(formatMoney(p.getLowestPriceUzs())).append(" so'm`\n")
                    .append("   🏪 ").append("ru".equals(lang) ? "Магазин: `" : "Do'kon: `").append(p.getStoreName()).append("`\n\n");
            if (count >= 5) break;
        }

        SendMessage msg = new SendMessage();
        msg.setChatId(chatId.toString());
        msg.setParseMode("Markdown");
        msg.setText(sb.toString());

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        List<InlineKeyboardButton> r = new ArrayList<>();
        InlineKeyboardButton btn = new InlineKeyboardButton();
        btn.setText("ru".equals(lang) ? "🚀 Смотреть всё в приложении" : "🚀 Barchasini Mini App'da Ko'rish");
        btn.setWebApp(new WebAppInfo(webappUrl));
        r.add(btn);
        rows.add(r);
        markup.setKeyboard(rows);
        msg.setReplyMarkup(markup);

        send(msg);
    }

    private void handleBuyerFavorites(Long chatId, org.telegram.telegrambots.meta.api.objects.User tgUser, String lang) {
        if (tgUser == null) return;
        Optional<com.priceiq.entity.User> userOpt = userRepository.findByTelegramId(tgUser.getId());
        if (userOpt.isEmpty()) {
            SendMessage msg = new SendMessage();
            msg.setChatId(chatId.toString());
            msg.setText("ru".equals(lang) ? "⭐ Ваш список избранного пуст." : "⭐ Sevimlilar ro'yxatingiz bo'sh.");
            send(msg);
            return;
        }

        List<Favorite> favs = favoriteRepository.findByUserId(userOpt.get().getId());
        if (favs.isEmpty()) {
            SendMessage msg = new SendMessage();
            msg.setChatId(chatId.toString());
            msg.setText("ru".equals(lang) ? "⭐ У вас пока нет сохраненных товаров в избранном." : "⭐ Sizda hali saqlangan sevimlilar mavjud emas.");
            send(msg);
            return;
        }

        StringBuilder sb = new StringBuilder();
        if ("ru".equals(lang)) {
            sb.append("⭐ *Ваши избранные товары:*\n\n");
        } else {
            sb.append("⭐ *Sizning Sevimli Mahsulotlaringiz:*\n\n");
        }

        int count = 0;
        for (Favorite f : favs) {
            count++;
            sb.append(count).append(". 📱 *").append(f.getProduct().getTitleUz()).append("*\n\n");
        }

        SendMessage msg = new SendMessage();
        msg.setChatId(chatId.toString());
        msg.setParseMode("Markdown");
        msg.setText(sb.toString());
        send(msg);
    }

    private void handleBuyerAlerts(Long chatId, org.telegram.telegrambots.meta.api.objects.User tgUser, String lang) {
        if (tgUser == null) return;
        Optional<com.priceiq.entity.User> userOpt = userRepository.findByTelegramId(tgUser.getId());
        if (userOpt.isEmpty()) {
            SendMessage msg = new SendMessage();
            msg.setChatId(chatId.toString());
            msg.setText("ru".equals(lang) ? "🔔 У вас нет активных уведомлений о снижении цен." : "🔔 Sizda faol narx alertlari mavjud emas.");
            send(msg);
            return;
        }

        List<PriceAlert> alerts = priceAlertRepository.findByUserId(userOpt.get().getId());
        if (alerts.isEmpty()) {
            SendMessage msg = new SendMessage();
            msg.setChatId(chatId.toString());
            msg.setText("ru".equals(lang) ? "🔔 У вас пока не настроены уведомления о снижении цен." : "🔔 Sizda hozircha narx tushishi xabarnomalari o'rnatilmagan.");
            send(msg);
            return;
        }

        StringBuilder sb = new StringBuilder();
        if ("ru".equals(lang)) {
            sb.append("🔔 *Ваши уведомления о ценах:*\n\n");
            for (PriceAlert a : alerts) {
                sb.append("• *").append(a.getProduct().getTitleUz()).append("*\n")
                        .append("  🎯 Целевая цена: `").append(formatMoney(a.getTargetPriceUzs())).append(" сум`\n\n");
            }
        } else {
            sb.append("🔔 *Sizning Narx Alertlaringiz:*\n\n");
            for (PriceAlert a : alerts) {
                sb.append("• *").append(a.getProduct().getTitleUz()).append("*\n")
                        .append("  🎯 Maqsadli narx: `").append(formatMoney(a.getTargetPriceUzs())).append(" so'm`\n\n");
            }
        }

        SendMessage msg = new SendMessage();
        msg.setChatId(chatId.toString());
        msg.setParseMode("Markdown");
        msg.setText(sb.toString());
        send(msg);
    }

    private void handleSupport(Long chatId, String lang) {
        SendMessage msg = new SendMessage();
        msg.setChatId(chatId.toString());
        msg.setParseMode("Markdown");
        if ("ru".equals(lang)) {
            msg.setText("💬 *Служба поддержки PRICEIQ*\n\n" +
                    "По любым вопросам или предложениям вы можете связаться с нами:\n\n" +
                    "👤 *Администратор:* @priceiq_admin\n" +
                    "🎧 *Бот поддержки:* @WearFlow_Support_Bot\n" +
                    "📞 *Телефон доверия:* +998 71 200 00 00\n" +
                    "🌐 *Сайт:* [priceiq.uz](https://frontend-three-gamma-ca7l713sls.vercel.app)");
        } else {
            msg.setText("💬 *PRICEIQ Qo'llab-quvvatlash xizmati*\n\n" +
                    "Savollar, takliflar yoki murojaatlar bo'yicha biz bilan bog'lanishingiz mumkin:\n\n" +
                    "👤 *Admin:* @priceiq_admin\n" +
                    "🎧 *Support Bot:* @WearFlow_Support_Bot\n" +
                    "📞 *Ishonch telefoni:* +998 71 200 00 00\n" +
                    "🌐 *Web:* [priceiq.uz](https://frontend-three-gamma-ca7l713sls.vercel.app)");
        }
        send(msg);
    }

    private void handleDefaultSearchOrHelp(Long chatId, String text, String lang) {
        List<ProductDto> products = productService.searchProducts(text);
        if (!products.isEmpty()) {
            ProductDto top = products.get(0);
            SendMessage msg = new SendMessage();
            msg.setChatId(chatId.toString());
            msg.setParseMode("Markdown");
            msg.setText("ru".equals(lang) ?
                    "🔍 *Результат поиска:*\n\n" +
                            "📱 *" + top.getTitleUz() + "*\n" +
                            "💰 Самая низкая цена: `" + formatMoney(top.getLowestPriceUzs()) + " сум`\n" +
                            "🏪 Магазин: `" + top.getStoreName() + "`" :
                    "🔍 *Qidiruv natijasi:*\n\n" +
                            "📱 *" + top.getTitleUz() + "*\n" +
                            "💰 Eng arzon narx: `" + formatMoney(top.getLowestPriceUzs()) + " so'm`\n" +
                            "🏪 Do'kon: `" + top.getStoreName() + "`");

            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> rows = new ArrayList<>();
            List<InlineKeyboardButton> r = new ArrayList<>();
            InlineKeyboardButton btn = new InlineKeyboardButton();
            btn.setText("ru".equals(lang) ? "📱 Открыть в приложении" : "📱 Mini App'da Ko'rish");
            btn.setWebApp(new WebAppInfo(webappUrl + "/product/" + top.getId()));
            r.add(btn);
            rows.add(r);
            markup.setKeyboard(rows);
            msg.setReplyMarkup(markup);

            send(msg);
        } else {
            SendMessage msg = new SendMessage();
            msg.setChatId(chatId.toString());
            msg.setText("ru".equals(lang) ?
                    "Нажмите /menu или воспользуйтесь кнопками меню ниже." :
                    "Buyruqni tanlash uchun /menu bosing yoki pastdagi menyu tugmalaridan foydalaning.");
            send(msg);
        }
    }

    // --- Helpers ---

    private Long parsePrice(String text) {
        if (text == null) return null;
        String clean = text.trim().toUpperCase();
        try {
            if (clean.contains("USD") || clean.contains("$")) {
                String numStr = clean.replaceAll("[^0-9.]", "");
                double usd = Double.parseDouble(numStr);
                return (long) (usd * 12800);
            }
            String numStr = clean.replaceAll("[^0-9]", "");
            if (numStr.isEmpty()) return null;
            return Long.parseLong(numStr);
        } catch (Exception e) {
            return null;
        }
    }

    private String formatMoney(Long amount) {
        if (amount == null) return "0";
        NumberFormat nf = NumberFormat.getInstance(new Locale("fr", "FR"));
        return nf.format(amount);
    }

    private void send(SendMessage msg) {
        try {
            execute(msg);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
