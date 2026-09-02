package com.priceiq.bot;

import com.priceiq.dto.ProductDto;
import com.priceiq.dto.UzumProductDto;
import com.priceiq.entity.*;
import com.priceiq.repository.*;
import com.priceiq.service.ProductService;
import com.priceiq.service.UserService;
import com.priceiq.service.UzumMarketService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.*;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Primary
public class SellerBotService extends TelegramLongPollingBot {

    @Value("${telegram.bot.username:princeiquz_bot}")
    private String botUsername;

    @Value("${telegram.bot.token:8603794898:AAEPq2YEv7OFBEoSkzYkrhiPe3JCPqcfDko}")
    private String botToken;

    @Value("${telegram.bot.webapp-url:https://frontend-three-gamma-ca7l713sls.vercel.app}")
    private String webappUrl;

    @Value("${telegram.support-bot.admin-chat-id:99887766}")
    private String adminChatId;

    private final ProductRepository productRepository;
    private final ProductOfferRepository offerRepository;
    private final PriceHistoryRepository priceHistoryRepository;
    private final CategoryRepository categoryRepository;
    private final StoreRepository storeRepository;
    private final UserRepository userRepository;
    private final FavoriteRepository favoriteRepository;
    private final PriceAlertRepository priceAlertRepository;
    private final SupportOperatorRepository supportOperatorRepository;
    private final SupportTicketRepository supportTicketRepository;
    private final UserService userService;
    private final ProductService productService;
    private final UzumMarketService uzumMarketService;

    // In-memory sessions by Chat ID
    private final Map<Long, SellerSession> sessions = new ConcurrentHashMap<>();
    private final Map<Long, String> userLanguageMap = new ConcurrentHashMap<>();

    // Map: operator forwarded message ID -> target user chat ID
    private final Map<Integer, Long> operatorMsgToUserChatMap = new ConcurrentHashMap<>();
    private static final Pattern CHAT_ID_PATTERN = Pattern.compile("(?:Chat ID|User ID):\\s*(\\d+)", Pattern.CASE_INSENSITIVE);

    public SellerBotService(ProductRepository productRepository,
                            ProductOfferRepository offerRepository,
                            PriceHistoryRepository priceHistoryRepository,
                            CategoryRepository categoryRepository,
                            StoreRepository storeRepository,
                            UserRepository userRepository,
                            FavoriteRepository favoriteRepository,
                            PriceAlertRepository priceAlertRepository,
                            SupportOperatorRepository supportOperatorRepository,
                            SupportTicketRepository supportTicketRepository,
                            UserService userService,
                            ProductService productService,
                            UzumMarketService uzumMarketService) {
        this.productRepository = productRepository;
        this.offerRepository = offerRepository;
        this.priceHistoryRepository = priceHistoryRepository;
        this.categoryRepository = categoryRepository;
        this.storeRepository = storeRepository;
        this.userRepository = userRepository;
        this.favoriteRepository = favoriteRepository;
        this.priceAlertRepository = priceAlertRepository;
        this.supportOperatorRepository = supportOperatorRepository;
        this.supportTicketRepository = supportTicketRepository;
        this.userService = userService;
        this.productService = productService;
        this.uzumMarketService = uzumMarketService;
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

        // 1. Check if an Operator or Admin is replying to a support ticket
        Optional<SupportOperator> opOpt = supportOperatorRepository.findByTelegramChatId(chatId);
        if (opOpt.isPresent() || isAdminChat(chatId)) {
            if (message.getReplyToMessage() != null) {
                handleOperatorReply(message, opOpt.orElse(null), lang);
                return;
            }
        }

        // 1.1. Check if a User is replying directly to a support response message
        if (message.getReplyToMessage() != null) {
            String repliedText = message.getReplyToMessage().getText();
            if (repliedText != null && (repliedText.contains("Qo'llab-quvvatlash") || repliedText.contains("поддержк") || repliedText.contains("Support") || repliedText.contains("PriceIQ"))) {
                session.setState(SellerState.AWAITING_SUPPORT_MESSAGE);
                forwardSupportMessageToOperators(message, tgUser, session, lang);
                return;
            }
        }

        // 2. Handle Contact sharing
        if (message.hasContact()) {
            handleContactReceived(chatId, message.getContact(), session, tgUser, lang);
            return;
        }

        // 3. Handle Media/Text when in AWAITING_SUPPORT_MESSAGE state
        if (session.getState() == SellerState.AWAITING_SUPPORT_MESSAGE) {
            if (message.hasText() && isCancelOrMenu(message.getText())) {
                session.setState(SellerState.MAIN_MENU);
                handleStart(chatId, session, tgUser, lang);
                return;
            }
            forwardSupportMessageToOperators(message, tgUser, session, lang);
            return;
        }

        // 3.1. Handle Operator Reply when in AWAITING_OPERATOR_REPLY state
        if (session.getState() == SellerState.AWAITING_OPERATOR_REPLY) {
            if (message.hasText() && isCancelOrMenu(message.getText())) {
                session.setState(SellerState.MAIN_MENU);
                session.setTempReplyToChatId(null);
                SendMessage cancelMsg = new SendMessage();
                cancelMsg.setChatId(chatId.toString());
                cancelMsg.setText("❌ Javob yozish bekor qilindi.");
                send(cancelMsg);
                return;
            }
            sendOperatorReplyToUser(message, session, tgUser, lang);
            return;
        }

        // 4. Handle Text Commands & Navigation
        if (message.hasText()) {
            String text = message.getText().trim();
            String norm = text.toLowerCase()
                    .replace("’", "'")
                    .replace("‘", "'")
                    .replace("`", "'")
                    .trim();

            // Language trigger commands
            if (norm.equals("/language") || norm.equals("/til") || norm.contains("tilni") || norm.contains("язык") || norm.contains("til tanlash")) {
                sendLanguageSelection(chatId);
                return;
            }

            // Reset / Logout / Re-auth commands
            if (norm.equals("/reset") || norm.equals("/logout") || norm.equals("/chiqish") || norm.equals("/login") || norm.contains("raqamni o'zgartirish") || norm.contains("qayta kirish") || norm.contains("сменить номер")) {
                handleResetPhone(chatId, session, tgUser, lang);
                return;
            }

            // Global Navigation & Cancellation
            if (norm.equals("/start") || norm.equals("/menu") || norm.contains("asosiy menyu") || norm.contains("главное меню") || norm.equals("/cancel") || norm.contains("bekor qilish") || norm.contains("отмена")) {
                session.setState(SellerState.MAIN_MENU);
                session.clearTempProductData();
                handleStart(chatId, session, tgUser, lang);
                return;
            }

            // If user typed a phone number as plain text
            if (isPhoneNumberFormat(text)) {
                processPhoneNumber(chatId, text, session, tgUser, lang);
                return;
            }

            // --- Support & Help ---
            if (norm.contains("qollab") || norm.contains("qo'llab") || norm.contains("quvvatlash") || norm.contains("yordam") || norm.contains("support") || norm.contains("поддержк") || norm.equals("/help") || norm.equals("/support")) {
                handleSupport(chatId, session, lang);
                return;
            }

            // --- Seller Actions (Only for store owners) ---
            if (norm.contains("yangi mahsulot") || norm.contains("mahsulot qo'sh") || norm.contains("добавить товар")) {
                startAddProductFlow(chatId, session, lang);
                return;
            }

            if (norm.contains("mening mahsulot") || norm.contains("мои товары") || norm.contains("mahsulotlarim")) {
                handleListMyProducts(chatId, session, lang);
                return;
            }

            if (norm.contains("narxni yangilash") || norm.contains("обновить цену") || norm.contains("narx yangilash")) {
                handleStartPriceUpdate(chatId, session, lang);
                return;
            }

            // --- Buyer / General Actions ---
            if (norm.contains("qidirish") || norm.contains("qidiruv") || norm.contains("поиск товаров") || norm.contains("поиск")) {
                SendMessage prompt = new SendMessage();
                prompt.setChatId(chatId.toString());
                prompt.setParseMode(null);
                prompt.setText("ru".equals(lang) ?
                        "🔍 Введите название товара (например: iPhone 16, Samsung TV, Холодильник):" :
                        "🔍 Qidirmoqchi bo'lgan mahsulot nomini yozing (masalan: iPhone 16, Samsung TV, Muzlatgich):");
                send(prompt);
                return;
            }

            if (norm.contains("takliflar") || norm.contains("taklif") || norm.contains("предложения") || norm.contains("скидк")) {
                handleTopDeals(chatId, lang);
                return;
            }

            if (norm.contains("sevimli") || norm.contains("избранн")) {
                handleBuyerFavorites(chatId, tgUser, lang);
                return;
            }

            if (norm.contains("alert") || norm.contains("ogohlantirish") || norm.contains("уведомлен")) {
                handleBuyerAlerts(chatId, tgUser, lang);
                return;
            }

            if (norm.contains("sozlama") || norm.contains("настройк") || norm.equals("/settings")) {
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

        // 5. Handle Photo Upload for Add Product
        if (message.hasPhoto() && session.getState() == SellerState.ADD_PRODUCT_PHOTO) {
            handleProductPhotoUpload(chatId, message.getPhoto(), session, lang);
        }
    }

    private boolean isCancelOrMenu(String text) {
        if (text == null) return false;
        String t = text.toLowerCase();
        return t.contains("asosiy menyu") || t.contains("главное меню") || t.contains("bekor qilish") || t.contains("отмена") || t.contains("suhbatni yakunlash") || t.contains("завершить") || t.equals("/start") || t.equals("/menu") || t.equals("/cancel");
    }

    private boolean isPhoneNumberFormat(String text) {
        if (text == null) return false;
        String clean = text.replaceAll("[^0-9]", "");
        return clean.length() >= 9 && clean.length() <= 13 && (text.startsWith("+") || text.startsWith("998") || clean.startsWith("998") || clean.length() == 9);
    }

    private boolean isAdminChat(Long chatId) {
        return adminChatId != null && adminChatId.equals(chatId.toString());
    }

    // --- Language Selection & Helpers ---

    private void sendLanguageSelection(Long chatId) {
        SendMessage msg = new SendMessage();
        msg.setChatId(chatId.toString());
        msg.setParseMode(null);
        msg.setText("🌐 Tilni tanlang / Выберите язык:");

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
        if (tgUser != null) {
            Optional<com.priceiq.entity.User> userOpt = userRepository.findByTelegramId(tgUser.getId());
            if (userOpt.isPresent() && userOpt.get().getLanguageCode() != null) {
                String l = userOpt.get().getLanguageCode().toLowerCase();
                String normalized = l.startsWith("ru") ? "ru" : "uz";
                userLanguageMap.put(chatId, normalized);
                return normalized;
            }
        }
        if (userLanguageMap.containsKey(chatId)) {
            String l = userLanguageMap.get(chatId).toLowerCase();
            return l.startsWith("ru") ? "ru" : "uz";
        }
        return "uz";
    }

    private void setUserLanguage(Long chatId, Long telegramId, String lang) {
        String normalized = (lang != null && lang.toLowerCase().startsWith("ru")) ? "ru" : "uz";
        userLanguageMap.put(chatId, normalized);
        userRepository.findByTelegramId(telegramId).ifPresent(user -> {
            user.setLanguageCode(normalized);
            userRepository.save(user);
        });
    }

    // --- Dynamic Store and Operator Lookup ---

    private Optional<Store> findActiveStoreForUser(Long chatId, String phone) {
        Optional<Store> storeOpt = storeRepository.findByOwnerChatId(chatId);
        if (storeOpt.isEmpty() && phone != null) {
            String clean = phone.replaceAll("[^0-9]", "");
            storeOpt = storeRepository.findByCleanPhone(clean);
            if (storeOpt.isEmpty()) {
                storeOpt = storeRepository.findByOwnerPhone("+" + clean);
            }
            if (storeOpt.isEmpty()) {
                storeOpt = storeRepository.findByOwnerPhone(phone);
            }
            storeOpt.ifPresent(s -> {
                s.setOwnerChatId(chatId);
                storeRepository.save(s);
            });
        }
        return storeOpt;
    }

    private Optional<SupportOperator> findActiveOperatorForUser(Long chatId, String phone) {
        Optional<SupportOperator> opOpt = supportOperatorRepository.findByTelegramChatId(chatId);
        if (opOpt.isEmpty() && phone != null) {
            String clean = phone.replaceAll("[^0-9]", "");
            opOpt = supportOperatorRepository.findByCleanPhone(clean);
            if (opOpt.isEmpty()) {
                opOpt = supportOperatorRepository.findByPhoneNumber("+" + clean);
            }
            if (opOpt.isEmpty()) {
                opOpt = supportOperatorRepository.findByPhoneNumber(phone);
            }
            opOpt.ifPresent(op -> {
                op.setTelegramChatId(chatId);
                op.setIsActive(true);
                supportOperatorRepository.save(op);
            });
        }
        return opOpt;
    }

    // --- Authentication & Phone Reset ---

    private void handleResetPhone(Long chatId, SellerSession session, org.telegram.telegrambots.meta.api.objects.User tgUser, String lang) {
        storeRepository.findByOwnerChatId(chatId).ifPresent(s -> {
            s.setOwnerChatId(null);
            storeRepository.save(s);
        });
        supportOperatorRepository.findByTelegramChatId(chatId).ifPresent(op -> {
            op.setTelegramChatId(null);
            supportOperatorRepository.save(op);
        });
        if (tgUser != null) {
            userRepository.findByTelegramId(tgUser.getId()).ifPresent(u -> {
                u.setPhoneNumber(null);
                userRepository.save(u);
            });
        }

        session.setPhoneNumber(null);
        session.setStore(null);
        session.setState(SellerState.AWAITING_CONTACT);

        promptContact(chatId, tgUser, lang);
    }

    private void handleStart(Long chatId, SellerSession session, org.telegram.telegrambots.meta.api.objects.User tgUser, String lang) {
        String name = tgUser != null && tgUser.getFirstName() != null ? tgUser.getFirstName() : "Foydalanuvchi";
        String phone = session.getPhoneNumber();
        if (phone == null && tgUser != null) {
            userRepository.findByTelegramId(tgUser.getId()).ifPresent(u -> session.setPhoneNumber(u.getPhoneNumber()));
            phone = session.getPhoneNumber();
        }

        // 1. If user is a Support Operator -> show Operator/Buyer menu (Never seller menu)
        Optional<SupportOperator> existingOp = findActiveOperatorForUser(chatId, phone);
        if (existingOp.isPresent()) {
            session.setPhoneNumber(existingOp.get().getPhoneNumber());
            session.setState(SellerState.MAIN_MENU);
            String welcomeOp = "ru".equals(lang) ?
                    "🎧 Здравствуйте, " + name + "!\n\nВы авторизованы как Support Оператор (" + existingOp.get().getFullName() + "). Обращения пользователей будут поступать в этот чат:" :
                    "🎧 Assalomu alaykum, " + name + "!\n\nSiz Support Operator (" + existingOp.get().getFullName() + ") sifatida tizimdasiz. Mijozlar murojaatlari ushbu chatga keladi:";
            sendBuyerMainMenu(chatId, welcomeOp, tgUser, lang);
            return;
        }

        // 2. Dynamic check if user is a Store Owner
        Optional<Store> existingStore = findActiveStoreForUser(chatId, phone);
        if (existingStore.isPresent()) {
            session.setStore(existingStore.get());
            session.setPhoneNumber(existingStore.get().getOwnerPhone());
            session.setState(SellerState.MAIN_MENU);
            String welcomeMsg = "ru".equals(lang) ?
                    "👋 Добро пожаловать, " + name + "!\n\n🏪 Ваш магазин: " + existingStore.get().getName() + "\n\nИспользуйте меню ниже для управления товарами:" :
                    "👋 Xush kelibsiz, " + name + "!\n\n🏪 Do'koningiz: " + existingStore.get().getName() + "\n\nQuyidagi menyu orqali mahsulotlaringizni boshqaring:";
            sendSellerMainMenu(chatId, welcomeMsg, session, lang);
            return;
        } else {
            session.setStore(null);
        }

        // 3. Check if user already shared phone before
        if (phone != null && !phone.isEmpty()) {
            session.setState(SellerState.MAIN_MENU);
            String welcomeBuyer = "ru".equals(lang) ?
                    "👋 Здравствуйте, " + name + "!\n\nДобро пожаловать в режим покупателя PRICEIQ. Вы можете искать и сравнивать лучшие цены:" :
                    "👋 Assalomu alaykum, " + name + "!\n\nPRICEIQ Xaridor rejimiga xush kelibsiz. Eng qulay narxlarni qidirishingiz va solishtirishingiz mumkin:";
            sendBuyerMainMenu(chatId, welcomeBuyer, tgUser, lang);
            return;
        }

        // 4. Prompt for Contact
        promptContact(chatId, tgUser, lang);
    }

    private void promptContact(Long chatId, org.telegram.telegrambots.meta.api.objects.User tgUser, String lang) {
        String name = tgUser != null && tgUser.getFirstName() != null ? tgUser.getFirstName() : "Foydalanuvchi";
        SendMessage msg = new SendMessage();
        msg.setChatId(chatId.toString());
        msg.setParseMode(null);

        if ("ru".equals(lang)) {
            msg.setText("👋 Здравствуйте, " + name + "!\n\n" +
                    "🛒 PRICEIQ — Умная платформа сравнения цен во всех магазинах Узбекистана.\n\n" +
                    "Чтобы подтвердить магазин (для продавцов), войти как оператор поддержки или привязать профиль, нажмите кнопку 📱 Отправить номер телефона или напишите номер сообщением:");
        } else {
            msg.setText("👋 Assalomu alaykum, " + name + "!\n\n" +
                    "🛒 PRICEIQ — O'zbekistondagi barcha do'konlar narxlarini solishtiruvchi aqlli platforma.\n\n" +
                    "Do'koningizni tasdiqlash (sotuvchilar uchun), support operator hisobini ulash yoki shaxsiy profilingizni faollashtirish uchun pastdagi 📱 Telefon Raqamni Yuborish tugmasini bosing yoki raqamingizni yozing:");
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
        processPhoneNumber(chatId, phone, session, tgUser, lang);
    }

    private void processPhoneNumber(Long chatId, String phone, SellerSession session, org.telegram.telegrambots.meta.api.objects.User tgUser, String lang) {
        if (phone == null) phone = "";
        String cleanPhone = phone.replaceAll("[^0-9]", "");
        String formattedPhone = phone.startsWith("+") ? phone : "+" + cleanPhone;

        if (tgUser != null) {
            userService.updatePhoneNumber(tgUser.getId(), formattedPhone, lang);
        }

        session.setPhoneNumber(formattedPhone);

        // 1. Check if phone belongs to Support Operator
        Optional<SupportOperator> opOpt = supportOperatorRepository.findByCleanPhone(cleanPhone);
        if (opOpt.isEmpty()) {
            opOpt = supportOperatorRepository.findByPhoneNumber(formattedPhone);
        }

        if (opOpt.isPresent()) {
            SupportOperator op = opOpt.get();
            op.setTelegramChatId(chatId);
            op.setIsActive(true);
            supportOperatorRepository.save(op);

            session.setStore(null);
            session.setState(SellerState.MAIN_MENU);

            String text = "ru".equals(lang) ?
                    "🎧 Вы успешно авторизовались как Support Оператор!\n\n" +
                            "👤 Оператор: " + op.getFullName() + "\n" +
                            "📞 Телефон: " + op.getPhoneNumber() + "\n\n" +
                            "Все обращения пользователей будут поступать в этот чат. Чтобы ответить, используйте функцию 'Reply'." :
                    "🎧 Siz Support Operator sifatida muvaffaqiyatli tizimga kirdingiz!\n\n" +
                            "👤 Operator: " + op.getFullName() + "\n" +
                            "📞 Telefon: " + op.getPhoneNumber() + "\n\n" +
                            "Foydalanuvchilardan kelgan murojaatlar ushbu chatga keladi. Javob berish uchun xabarga 'Reply' qiling.";

            sendBuyerMainMenu(chatId, text, tgUser, lang);
            return;
        }

        // 2. Check if phone belongs to a Store Owner
        Optional<Store> storeOpt = storeRepository.findByCleanPhone(cleanPhone);
        if (storeOpt.isEmpty()) {
            storeOpt = storeRepository.findByOwnerPhone(formattedPhone);
        }

        if (storeOpt.isPresent()) {
            Store store = storeOpt.get();
            store.setOwnerChatId(chatId);
            storeRepository.save(store);

            session.setStore(store);
            session.setState(SellerState.MAIN_MENU);

            String text = "ru".equals(lang) ?
                    "✅ Поздравляем, ваш магазин успешно подключен!\n\n🏪 Магазин: " + store.getName() + "\n📞 Телефон: " + formattedPhone + "\n\nИспользуйте меню ниже для добавления товаров и управления ценами:" :
                    "✅ Tabriklaymiz, Do'koningiz Muvaffaqiyatli Ulandi!\n\n🏪 Do'kon: " + store.getName() + "\n📞 Telefon: " + formattedPhone + "\n\nEndi quyidagi menyu orqali yangi mahsulot qo'shishingiz va narxlarni boshqarishingiz mumkin:";
            sendSellerMainMenu(chatId, text, session, lang);
            return;
        }

        // 3. Regular Buyer
        session.setStore(null);
        session.setState(SellerState.MAIN_MENU);

        String text = "ru".equals(lang) ?
                "✅ Ваш номер телефона успешно сохранен!\n\nВы находитесь в режиме Покупателя. Вы можете искать самые низкие цены и следить за скидками:" :
                "✅ Telefon raqamingiz muvaffaqiyatli saqlandi!\n\nSiz PRICEIQ Xaridor rejimidasiz. Barcha do'konlardagi eng arzon narxlarni qidirishingiz va narx tushishini kuzatishingiz mumkin:";
        sendBuyerMainMenu(chatId, text, tgUser, lang);
    }

    // --- Direct In-Bot Support Flow ---

    private void handleSupport(Long chatId, SellerSession session, String lang) {
        session.setState(SellerState.AWAITING_SUPPORT_MESSAGE);

        SendMessage msg = new SendMessage();
        msg.setChatId(chatId.toString());
        msg.setParseMode(null);

        if ("ru".equals(lang)) {
            msg.setText("🎧 PRICEIQ Служба поддержки\n\n" +
                    "✍️ Напишите ваше сообщение, вопрос или жалобу прямо сюда.\n" +
                    "Вы можете отправить текст, фото, видео или голосовое сообщение.\n\n" +
                    "Операторы поддержки ответят вам прямо в этом чате!\n\n" +
                    "📞 Телефон доверия: +998 71 200 00 00\n" +
                    "👤 Администратор: @priceiq_admin\n\n" +
                    "👇 Отправьте ваше сообщение:");
        } else {
            msg.setText("🎧 PRICEIQ Qo'llab-quvvatlash xizmati\n\n" +
                    "✍️ Murojaatingiz, savolingiz yoki xabaringizni shu yerga yozib qoldiring.\n" +
                    "Matn, rasm, video yoki ovozli xabar yuborishingiz mumkin.\n\n" +
                    "Support operatorlarimiz to'g'ridan-to'g'ri ushbu chat orqali javob berishadi!\n\n" +
                    "📞 Ishonch telefoni: +998 71 200 00 00\n" +
                    "👤 Administrator: @priceiq_admin\n\n" +
                    "👇 Xabaringizni yozib yuboring:");
        }

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);
        List<KeyboardRow> rows = new ArrayList<>();
        KeyboardRow r = new KeyboardRow();
        r.add(new KeyboardButton("ru".equals(lang) ? "🏠 Главное меню / Отмена" : "🏠 Asosiy Menyu / Bekor qilish"));
        rows.add(r);
        keyboardMarkup.setKeyboard(rows);
        msg.setReplyMarkup(keyboardMarkup);

        send(msg);
    }

    private void forwardSupportMessageToOperators(Message message, org.telegram.telegrambots.meta.api.objects.User tgUser, SellerSession session, String lang) {
        Long userChatId = message.getChatId();
        String userName = tgUser != null ? (tgUser.getFirstName() + (tgUser.getLastName() != null ? " " + tgUser.getLastName() : "")) : "Foydalanuvchi";
        String userHandle = tgUser != null && tgUser.getUserName() != null ? "@" + tgUser.getUserName() : "mavjud emas";
        String phone = session.getPhoneNumber() != null ? session.getPhoneNumber() : "mavjud emas";

        Optional<Store> st = findActiveStoreForUser(userChatId, phone);
        String roleStr = st.isPresent() ? "Do'kon Sotuvchisi (" + st.get().getName() + ")" : "Xaridor (Buyer)";

        String msgContent = message.hasText() ? message.getText() : (message.getCaption() != null ? message.getCaption() : "[Media Xabar]");
        String mediaType = message.hasPhoto() ? "PHOTO" : (message.hasVideo() ? "VIDEO" : (message.hasVoice() ? "VOICE" : (message.hasDocument() ? "DOCUMENT" : "TEXT")));

        // 1. Save Support Ticket to Database for Admin Web Panel
        try {
            SupportTicket ticket = new SupportTicket(
                    userChatId,
                    userName + " (" + userHandle + ")",
                    phone,
                    roleStr,
                    msgContent,
                    mediaType
            );
            supportTicketRepository.save(ticket);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 2. Forward to all active support operators
        String header = "📩 Yangi Murojaat! / Новое обращение!\n" +
                "👤 Foydalanuvchi: " + userName + " (" + userHandle + ")\n" +
                "🆔 Chat ID: " + userChatId + "\n" +
                "📞 Telefon: " + phone + "\n" +
                "🎭 Rol: " + roleStr + "\n" +
                "──────────────────\n";

        List<SupportOperator> activeOperators = supportOperatorRepository.findByIsActiveTrue();
        Set<Long> targetOperatorChatIds = new HashSet<>();

        for (SupportOperator op : activeOperators) {
            if (op.getTelegramChatId() != null) {
                targetOperatorChatIds.add(op.getTelegramChatId());
            }
        }

        if (adminChatId != null && !adminChatId.isEmpty()) {
            try {
                Long admId = Long.parseLong(adminChatId.trim());
                targetOperatorChatIds.add(admId);
            } catch (Exception ignored) {}
        }

        InlineKeyboardMarkup replyMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        List<InlineKeyboardButton> r = new ArrayList<>();
        InlineKeyboardButton replyBtn = new InlineKeyboardButton();
        replyBtn.setText("💬 Javob Yozish / Ответить");
        replyBtn.setCallbackData("reply_ticket_" + userChatId);
        r.add(replyBtn);
        rows.add(r);
        replyMarkup.setKeyboard(rows);

        for (Long opChatId : targetOperatorChatIds) {
            try {
                if (message.hasText()) {
                    SendMessage fwd = new SendMessage();
                    fwd.setChatId(opChatId.toString());
                    fwd.setParseMode(null);
                    fwd.setText(header + "💬 Xabar: " + message.getText());
                    fwd.setReplyMarkup(replyMarkup);
                    Message sent = execute(fwd);
                    operatorMsgToUserChatMap.put(sent.getMessageId(), userChatId);
                } else if (message.hasPhoto()) {
                    SendPhoto fwd = new SendPhoto();
                    fwd.setChatId(opChatId.toString());
                    fwd.setPhoto(new InputFile(message.getPhoto().get(message.getPhoto().size() - 1).getFileId()));
                    fwd.setCaption(header + "📷 Rasm: " + (message.getCaption() != null ? message.getCaption() : ""));
                    fwd.setParseMode(null);
                    fwd.setReplyMarkup(replyMarkup);
                    Message sent = execute(fwd);
                    operatorMsgToUserChatMap.put(sent.getMessageId(), userChatId);
                } else if (message.hasVideo()) {
                    SendVideo fwd = new SendVideo();
                    fwd.setChatId(opChatId.toString());
                    fwd.setVideo(new InputFile(message.getVideo().getFileId()));
                    fwd.setCaption(header + "🎥 Video: " + (message.getCaption() != null ? message.getCaption() : ""));
                    fwd.setParseMode(null);
                    fwd.setReplyMarkup(replyMarkup);
                    Message sent = execute(fwd);
                    operatorMsgToUserChatMap.put(sent.getMessageId(), userChatId);
                } else if (message.hasVoice()) {
                    SendVoice fwd = new SendVoice();
                    fwd.setChatId(opChatId.toString());
                    fwd.setVoice(new InputFile(message.getVoice().getFileId()));
                    fwd.setCaption(header + "🎤 Ovozli xabar");
                    fwd.setParseMode(null);
                    fwd.setReplyMarkup(replyMarkup);
                    Message sent = execute(fwd);
                    operatorMsgToUserChatMap.put(sent.getMessageId(), userChatId);
                } else if (message.hasDocument()) {
                    SendDocument fwd = new SendDocument();
                    fwd.setChatId(opChatId.toString());
                    fwd.setDocument(new InputFile(message.getDocument().getFileId()));
                    fwd.setCaption(header + "📄 Hujjat: " + (message.getCaption() != null ? message.getCaption() : ""));
                    fwd.setParseMode(null);
                    fwd.setReplyMarkup(replyMarkup);
                    Message sent = execute(fwd);
                    operatorMsgToUserChatMap.put(sent.getMessageId(), userChatId);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Set all active operators into live chat mode with this user!
        for (Long opChatId : targetOperatorChatIds) {
            SellerSession opSession = sessions.computeIfAbsent(opChatId, id -> new SellerSession(opChatId, opChatId));
            opSession.setTempReplyToChatId(userChatId);
            opSession.setState(SellerState.AWAITING_OPERATOR_REPLY);
        }

        // Maintain active live chat mode for user
        session.setState(SellerState.AWAITING_SUPPORT_MESSAGE);

        SendMessage confirmMsg = new SendMessage();
        confirmMsg.setChatId(userChatId.toString());
        confirmMsg.setParseMode(null);
        confirmMsg.setText("ru".equals(lang) ?
                "✅ Ваше сообщение передано оператору поддержки!\n\nВы можете продолжать писать прямо сюда. Для выхода нажмите '🏠 Главное меню'." :
                "✅ Xabaringiz support operatorga yetkazildi!\n\nSuhbatni shu yerda davom ettirishingiz mumkin. Menyuga qaytish uchun '🏠 Asosiy Menyu' tugmasini bosing.");

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);
        List<KeyboardRow> kbRows = new ArrayList<>();
        KeyboardRow kbRow = new KeyboardRow();
        kbRow.add(new KeyboardButton("ru".equals(lang) ? "🏠 Главное меню / Завершить" : "🏠 Asosiy Menyu / Suhbatni yakunlash"));
        kbRows.add(kbRow);
        keyboardMarkup.setKeyboard(kbRows);
        confirmMsg.setReplyMarkup(keyboardMarkup);

        send(confirmMsg);
    }

    private void handleOperatorReply(Message message, SupportOperator operator, String lang) {
        Message repliedTo = message.getReplyToMessage();
        Long targetUserChatId = operatorMsgToUserChatMap.get(repliedTo.getMessageId());

        if (targetUserChatId == null) {
            targetUserChatId = extractTargetChatId(repliedTo);
        }

        if (targetUserChatId == null) {
            SendMessage err = new SendMessage();
            err.setChatId(message.getChatId().toString());
            err.setText("⚠️ Ushbu xabardan murojaatchining Chat ID si topilmadi. Iltimos, xabardagi '💬 Javob Yozish' tugmasini bosing yoki xabarga to'g'ridan-to'g'ri Reply qiling.");
            send(err);
            return;
        }

        String operatorName = operator != null ? operator.getFullName() : "Administrator";

        // Update Ticket in database
        try {
            supportTicketRepository.findTopByUserChatIdAndStatusOrderByCreatedAtDesc(targetUserChatId, "PENDING")
                    .ifPresent(t -> {
                        t.setStatus("ANSWERED");
                        t.setOperatorName(operatorName);
                        t.setOperatorChatId(message.getChatId());
                        t.setReplyText(message.getText());
                        t.setRepliedAt(LocalDateTime.now());
                        supportTicketRepository.save(t);
                    });
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            SendMessage toUser = new SendMessage();
            toUser.setChatId(targetUserChatId.toString());
            toUser.setParseMode(null);
            toUser.setText("👨‍💻 Qo'llab-quvvatlash xizmati javobi (" + operatorName + "):\n\n" + message.getText());
            execute(toUser);

            SendMessage ack = new SendMessage();
            ack.setChatId(message.getChatId().toString());
            ack.setText("✅ Javobingiz foydalanuvchiga (Chat ID: " + targetUserChatId + ") yetkazildi! Yana xabar yozishingiz mumkin.");
            send(ack);

            // CRITICAL: Set the user's session into AWAITING_SUPPORT_MESSAGE so their follow-up is routed back!
            final Long finalUserChatId = targetUserChatId;
            SellerSession userSession = sessions.computeIfAbsent(finalUserChatId, id -> new SellerSession(id, id));
            userSession.setState(SellerState.AWAITING_SUPPORT_MESSAGE);
        } catch (Exception e) {
            SendMessage err = new SendMessage();
            err.setChatId(message.getChatId().toString());
            err.setText("❌ Foydalanuvchiga javob yuborishda xatolik: " + e.getMessage());
            send(err);
        }
    }

    private void sendOperatorReplyToUser(Message message, SellerSession session, org.telegram.telegrambots.meta.api.objects.User tgUser, String lang) {
        Long targetUserChatId = session.getTempReplyToChatId();
        if (targetUserChatId == null) {
            session.setState(SellerState.MAIN_MENU);
            return;
        }

        Optional<SupportOperator> opOpt = supportOperatorRepository.findByTelegramChatId(message.getChatId());
        String operatorName = opOpt.isPresent() ? opOpt.get().getFullName() : (tgUser != null && tgUser.getFirstName() != null ? tgUser.getFirstName() : "Support Operator");

        String replyText = message.hasText() ? message.getText() : (message.getCaption() != null ? message.getCaption() : "[Media javob]");

        // Update Ticket in database
        try {
            supportTicketRepository.findTopByUserChatIdAndStatusOrderByCreatedAtDesc(targetUserChatId, "PENDING")
                    .ifPresent(t -> {
                        t.setStatus("ANSWERED");
                        t.setOperatorName(operatorName);
                        t.setOperatorChatId(message.getChatId());
                        t.setReplyText(replyText);
                        t.setRepliedAt(LocalDateTime.now());
                        supportTicketRepository.save(t);
                    });
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Deliver to user
        try {
            if (message.hasText()) {
                SendMessage toUser = new SendMessage();
                toUser.setChatId(targetUserChatId.toString());
                toUser.setParseMode(null);
                toUser.setText("👨‍💻 Qo'llab-quvvatlash xizmati javobi (" + operatorName + "):\n\n" + message.getText());
                execute(toUser);
            } else if (message.hasPhoto()) {
                SendPhoto toUser = new SendPhoto();
                toUser.setChatId(targetUserChatId.toString());
                toUser.setPhoto(new InputFile(message.getPhoto().get(message.getPhoto().size() - 1).getFileId()));
                toUser.setCaption("👨‍💻 Qo'llab-quvvatlash xizmati javobi (" + operatorName + "):\n\n" + (message.getCaption() != null ? message.getCaption() : ""));
                toUser.setParseMode(null);
                execute(toUser);
            } else if (message.hasVoice()) {
                SendVoice toUser = new SendVoice();
                toUser.setChatId(targetUserChatId.toString());
                toUser.setVoice(new InputFile(message.getVoice().getFileId()));
                toUser.setCaption("👨‍💻 Qo'llab-quvvatlash xizmati javobi (" + operatorName + ")");
                toUser.setParseMode(null);
                execute(toUser);
            }

            SendMessage ack = new SendMessage();
            ack.setChatId(message.getChatId().toString());
            ack.setText("✅ Javobingiz foydalanuvchiga (Chat ID: " + targetUserChatId + ") yetkazildi! Suhbatni davom ettirishingiz mumkin.");
            send(ack);

            // CRITICAL: Set the user's session into AWAITING_SUPPORT_MESSAGE so their follow-up is routed back!
            final Long finalUserChatId = targetUserChatId;
            SellerSession userSession = sessions.computeIfAbsent(finalUserChatId, id -> new SellerSession(id, id));
            userSession.setState(SellerState.AWAITING_SUPPORT_MESSAGE);
        } catch (Exception e) {
            SendMessage err = new SendMessage();
            err.setChatId(message.getChatId().toString());
            err.setText("❌ Foydalanuvchiga javob yuborishda xatolik: " + e.getMessage());
            send(err);
        }

        // Keep operator in live chat mode with this user!
        session.setState(SellerState.AWAITING_OPERATOR_REPLY);
    }

    private Long extractTargetChatId(Message repliedTo) {
        String text = repliedTo.getText() != null ? repliedTo.getText() : repliedTo.getCaption();
        if (text == null) return null;
        Matcher m = CHAT_ID_PATTERN.matcher(text);
        if (m.find()) {
            try {
                return Long.parseLong(m.group(1));
            } catch (Exception ignored) {}
        }
        return null;
    }

    // --- Main Menus (Seller vs Buyer) ---

    private void sendSellerMainMenu(Long chatId, String text, SellerSession session, String lang) {
        SendMessage msg = new SendMessage();
        msg.setChatId(chatId.toString());
        msg.setText(text);
        msg.setParseMode(null);

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);

        List<KeyboardRow> rows = new ArrayList<>();

        KeyboardRow r1 = new KeyboardRow();
        r1.add(new KeyboardButton("ru".equals(lang) ? "➕ Добавить товар" : "➕ Yangi Mahsulot Qo'shish"));
        r1.add(new KeyboardButton("ru".equals(lang) ? "📦 Мои товары" : "📦 Mening Mahsulotlarim"));

        KeyboardRow r2 = new KeyboardRow();
        r2.add(new KeyboardButton("ru".equals(lang) ? "✏️ Обновить цену" : "✏️ Narxni Yangilash"));
        r2.add(new KeyboardButton("ru".equals(lang) ? "🌐 Выбрать язык" : "🌐 Tilni tanlash"));

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
        msg.setParseMode(null);

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
        r3.add(new KeyboardButton("ru".equals(lang) ? "📞 Поддержка" : "📞 Qo'llab-quvvatlash"));
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
        msg.setParseMode(null);
        msg.setText("ru".equals(lang) ?
                "⚙️ Раздел настроек:\n\nВыберите нужное действие:" :
                "⚙️ Sozlamalar bo'limi:\n\nQuyidagi amallardan birini tanlang:");

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        List<InlineKeyboardButton> r1 = new ArrayList<>();
        InlineKeyboardButton langBtn = new InlineKeyboardButton();
        langBtn.setText("ru".equals(lang) ? "🌐 Сменить язык (UZ / RU)" : "🌐 Tilni o'zgartirish (UZ / RU)");
        langBtn.setCallbackData("settings_lang");
        r1.add(langBtn);

        List<InlineKeyboardButton> r2 = new ArrayList<>();
        InlineKeyboardButton profileBtn = new InlineKeyboardButton();
        profileBtn.setText("ru".equals(lang) ? "👤 Мой профиль" : "👤 Mening profilim");
        profileBtn.setCallbackData("settings_profile");
        r2.add(profileBtn);

        List<InlineKeyboardButton> r3 = new ArrayList<>();
        InlineKeyboardButton resetBtn = new InlineKeyboardButton();
        resetBtn.setText("ru".equals(lang) ? "🚪 Сменить номер / Выйти" : "🚪 Raqamni o'zgartirish / Qayta kirish");
        resetBtn.setCallbackData("settings_reset_phone");
        r3.add(resetBtn);

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
        Optional<Store> storeOpt = findActiveStoreForUser(chatId, session.getPhoneNumber());
        if (storeOpt.isEmpty()) {
            SendMessage msg = new SendMessage();
            msg.setChatId(chatId.toString());
            msg.setText("ru".equals(lang) ? "⚠️ Магазин не привязан к вашему номеру." : "⚠️ Sizning raqamingizga biriktirilgan do'kon topilmadi.");
            send(msg);
            handleStart(chatId, session, null, lang);
            return;
        }

        session.setStore(storeOpt.get());
        session.clearTempProductData();
        session.setState(SellerState.ADD_PRODUCT_PHOTO);

        SendMessage msg = new SendMessage();
        msg.setChatId(chatId.toString());
        msg.setParseMode(null);
        msg.setText("ru".equals(lang) ?
                "📸 Шаг 1: Отправьте фото товара.\n\nОтправьте фото, ссылку на изображение или нажмите 'Пропустить':" :
                "📸 1-qadam: Mahsulot rasmini yuboring.\n\nRasm faylini yuboring yoki rasm havolasini (URL) yozing (yoki 'O'tkazib yuborish' deb yozing):");

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
        msg.setParseMode(null);
        msg.setText("ru".equals(lang) ?
                "📝 Шаг 2: Введите название товара.\n\nПример: iPhone 16 Pro Max 256GB или Телевизор Samsung 55\":" :
                "📝 2-qadam: Mahsulot nomini kiriting.\n\nMisol: iPhone 16 Pro Max 256GB yoki Artel Kir yuvish mashinasi:");

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
        msg.setParseMode(null);
        msg.setText("ru".equals(lang) ?
                "✅ Фото принято!\n\n📝 Шаг 2: Введите название товара.\n\nПример: iPhone 16 Pro Max 256GB или Samsung TV 55\":" :
                "✅ Rasm qabul qilindi!\n\n📝 2-qadam: Mahsulot nomini kiriting.\n\nMisol: iPhone 16 Pro Max 256GB yoki Samsung TV 55\":");
        send(msg);
    }

    private void handleProductNameInput(Long chatId, String text, SellerSession session, String lang) {
        session.setTempTitle(text);
        session.setState(SellerState.ADD_PRODUCT_PRICE);

        SendMessage msg = new SendMessage();
        msg.setChatId(chatId.toString());
        msg.setParseMode(null);
        msg.setText("ru".equals(lang) ?
                "💰 Шаг 3: Введите цену товара.\n\nПример: 15 000 000 или 1200 USD yoki 8500000:" :
                "💰 3-qadam: Mahsulot narxini kiriting.\n\nMisol: 15 000 000 yoki 1200 USD yoki 8500000:");
        send(msg);
    }

    private void handleProductPriceInput(Long chatId, String text, SellerSession session, String lang) {
        Long priceUzs = parsePrice(text);
        if (priceUzs == null || priceUzs <= 0) {
            SendMessage msg = new SendMessage();
            msg.setChatId(chatId.toString());
            msg.setText("ru".equals(lang) ?
                    "❌ Неверная цена. Введите только цифры (например: 12500000 yoki 1000 USD):" :
                    "❌ Narx noto'g'ri kiritildi. Faqat son kiriting (masalan: 12500000 yoki 1000 USD):");
            send(msg);
            return;
        }

        session.setTempPriceUzs(priceUzs);
        session.setState(SellerState.ADD_PRODUCT_DESCRIPTION);

        SendMessage msg = new SendMessage();
        msg.setChatId(chatId.toString());
        msg.setParseMode(null);
        msg.setText("ru".equals(lang) ?
                "📄 Шаг 4: Введите краткое описание / комментарий к товару.\n\n(Гарантия, характеристики или нажмите 'Пропустить'):" :
                "📄 4-qadam: Mahsulot haqida qisqacha tavsif / izoh kiriting.\n\n(Kafolat, xususiyatlar yoki 'O'tkazib yuborish' tugmasini bosing):");

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

        String storeName = session.getStore() != null ? session.getStore().getName() : "Store";

        String caption = "ru".equals(lang) ?
                "📋 Шаг 5: Подтвердите данные товара:\n\n" +
                        "🏷️ Название: " + session.getTempTitle() + "\n" +
                        "🏪 Магазин: " + storeName + "\n" +
                        "💰 Цена: " + formatMoney(session.getTempPriceUzs()) + " сум\n" +
                        "📄 Описание: " + session.getTempDescription() + "\n\n" +
                        "Все данные верны?" :
                "📋 5-qadam: Mahsulot ma'lumotlarini tasdiqlang:\n\n" +
                        "🏷️ Nomi: " + session.getTempTitle() + "\n" +
                        "🏪 Do'kon: " + storeName + "\n" +
                        "💰 Narxi: " + formatMoney(session.getTempPriceUzs()) + " so'm\n" +
                        "📄 Tavsif: " + session.getTempDescription() + "\n\n" +
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
            sendPhoto.setParseMode(null);
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
        msg.setParseMode(null);
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

        if ("set_lang_uz".equals(data)) {
            setUserLanguage(chatId, tgUser.getId(), "uz");
            SendMessage msg = new SendMessage();
            msg.setChatId(chatId.toString());
            msg.setText("✅ Til muvaffaqiyatli O'zbekchaga o'rnatildi!");
            msg.setParseMode(null);
            send(msg);
            Optional<SupportOperator> op = findActiveOperatorForUser(chatId, session.getPhoneNumber());
            Optional<Store> st = findActiveStoreForUser(chatId, session.getPhoneNumber());
            if (op.isPresent()) {
                sendBuyerMainMenu(chatId, "Asosiy menyu:", tgUser, "uz");
            } else if (st.isPresent()) {
                session.setStore(st.get());
                sendSellerMainMenu(chatId, "Asosiy menyu:", session, "uz");
            } else {
                sendBuyerMainMenu(chatId, "Asosiy menyu:", tgUser, "uz");
            }
        } else if ("set_lang_ru".equals(data)) {
            setUserLanguage(chatId, tgUser.getId(), "ru");
            SendMessage msg = new SendMessage();
            msg.setChatId(chatId.toString());
            msg.setText("✅ Язык успешно установлен на Русский!");
            msg.setParseMode(null);
            send(msg);
            Optional<SupportOperator> op = findActiveOperatorForUser(chatId, session.getPhoneNumber());
            Optional<Store> st = findActiveStoreForUser(chatId, session.getPhoneNumber());
            if (op.isPresent()) {
                sendBuyerMainMenu(chatId, "Главное меню:", tgUser, "ru");
            } else if (st.isPresent()) {
                session.setStore(st.get());
                sendSellerMainMenu(chatId, "Главное меню:", session, "ru");
            } else {
                sendBuyerMainMenu(chatId, "Главное меню:", tgUser, "ru");
            }
        } else if ("confirm_add_product".equals(data)) {
            String currentLang = getUserLanguage(chatId, tgUser);
            saveNewProductToDatabase(chatId, session, messageId, currentLang);
        } else if ("cancel_add_product".equals(data)) {
            String currentLang = getUserLanguage(chatId, tgUser);
            session.clearTempProductData();
            session.setState(SellerState.MAIN_MENU);
            sendSellerMainMenu(chatId, "ru".equals(currentLang) ? "❌ Добавление товара отменено." : "❌ Mahsulot qo'shish bekor qilindi.", session, currentLang);
        } else if (data.startsWith("update_price_")) {
            String currentLang = getUserLanguage(chatId, tgUser);
            Long productId = Long.parseLong(data.replace("update_price_", ""));
            session.setTempSelectedProductId(productId);
            session.setState(SellerState.UPDATE_PRICE_ENTER);

            SendMessage msg = new SendMessage();
            msg.setChatId(chatId.toString());
            msg.setParseMode(null);
            msg.setText("ru".equals(currentLang) ?
                    "💰 Введите новую цену для выбранного товара (в сумах):" :
                    "💰 Tanlangan mahsulot uchun yangi narxni kiriting (so'mda):");
            send(msg);
        } else if ("settings_lang".equals(data)) {
            sendLanguageSelection(chatId);
        } else if ("settings_profile".equals(data)) {
            String currentLang = getUserLanguage(chatId, tgUser);
            String name = tgUser.getFirstName() != null ? tgUser.getFirstName() : "Foydalanuvchi";
            String username = tgUser.getUserName() != null ? "@" + tgUser.getUserName() : "kiritilmagan";
            String phone = session.getPhoneNumber() != null ? session.getPhoneNumber() : "kiritilmagan";

            Optional<Store> storeOpt = findActiveStoreForUser(chatId, phone);
            Optional<SupportOperator> opOpt = findActiveOperatorForUser(chatId, phone);

            String role;
            if (opOpt.isPresent()) {
                role = "ru".equals(currentLang) ? "🎧 Support Оператор (" + opOpt.get().getFullName() + ")" : "🎧 Support Operator (" + opOpt.get().getFullName() + ")";
            } else if (storeOpt.isPresent()) {
                role = "ru".equals(currentLang) ? "🏪 Продавец магазина (" + storeOpt.get().getName() + ")" : "🏪 Do'kon Sotuvchisi (" + storeOpt.get().getName() + ")";
            } else {
                role = "ru".equals(currentLang) ? "🛍️ Покупатель (Buyer)" : "🛍️ Oddiy Xaridor (Buyer)";
            }

            SendMessage msg = new SendMessage();
            msg.setChatId(chatId.toString());
            msg.setParseMode(null);
            msg.setText("ru".equals(currentLang) ?
                    "👤 Профиль пользователя:\n\n" +
                            "🏷️ Имя: " + name + "\n" +
                            "🆔 Telegram ID: " + tgUser.getId() + "\n" +
                            "🔗 Username: " + username + "\n" +
                            "📞 Телефон: " + phone + "\n" +
                            "🎭 Роль: " + role :
                    "👤 Foydalanuvchi Profili:\n\n" +
                            "🏷️ Ism: " + name + "\n" +
                            "🆔 Telegram ID: " + tgUser.getId() + "\n" +
                            "🔗 Username: " + username + "\n" +
                            "📞 Telefon: " + phone + "\n" +
                            "🎭 Rol: " + role);
            send(msg);
        } else if ("settings_reset_phone".equals(data)) {
            String currentLang = getUserLanguage(chatId, tgUser);
            handleResetPhone(chatId, session, tgUser, currentLang);
        } else if (data.startsWith("reply_ticket_")) {
            Long targetChatId = Long.parseLong(data.replace("reply_ticket_", ""));
            session.setState(SellerState.AWAITING_OPERATOR_REPLY);
            session.setTempReplyToChatId(targetChatId);

            SendMessage prompt = new SendMessage();
            prompt.setChatId(chatId.toString());
            prompt.setParseMode(null);
            prompt.setText("✍️ Foydalanuvchiga (Chat ID: " + targetChatId + ") javob xabaringizni yozib yuboring (matn, rasm yoki ovozli xabar):\n\n(Bekor qilish uchun /cancel yoki 'Bekor qilish' deb yozing)");
            send(prompt);
        } else if ("settings_back".equals(data)) {
            String currentLang = getUserLanguage(chatId, tgUser);
            Optional<SupportOperator> op = findActiveOperatorForUser(chatId, session.getPhoneNumber());
            Optional<Store> st = findActiveStoreForUser(chatId, session.getPhoneNumber());
            if (op.isPresent()) {
                sendBuyerMainMenu(chatId, "ru".equals(currentLang) ? "Главное меню:" : "Asosiy menyu:", tgUser, currentLang);
            } else if (st.isPresent()) {
                session.setStore(st.get());
                sendSellerMainMenu(chatId, "ru".equals(currentLang) ? "Главное меню:" : "Asosiy menyu:", session, currentLang);
            } else {
                sendBuyerMainMenu(chatId, "ru".equals(currentLang) ? "Главное меню:" : "Asosiy menyu:", tgUser, currentLang);
            }
        }
    }

    private void saveNewProductToDatabase(Long chatId, SellerSession session, Integer messageId, String lang) {
        Optional<Store> storeOpt = findActiveStoreForUser(chatId, session.getPhoneNumber());
        if (storeOpt.isEmpty() || session.getTempTitle() == null || session.getTempPriceUzs() == null) {
            SendMessage msg = new SendMessage();
            msg.setChatId(chatId.toString());
            msg.setText("ru".equals(lang) ? "⚠️ Данные сессии не найдены. Попробуйте снова." : "⚠️ Sessiya ma'lumotlari topilmadi. Qaytadan urinib ko'ring.");
            send(msg);
            return;
        }

        Store store = storeOpt.get();

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
                    store,
                    session.getTempPriceUzs(),
                    (long) (session.getTempPriceUzs() * 1.05),
                    true,
                    store.getWebsiteUrl() != null ? store.getWebsiteUrl() : "https://uzum.uz"
            );
            offerRepository.save(offer);

            priceHistoryRepository.save(new PriceHistory(null, product, session.getTempPriceUzs(), LocalDateTime.now()));

            session.clearTempProductData();
            session.setState(SellerState.MAIN_MENU);

            SendMessage successMsg = new SendMessage();
            successMsg.setChatId(chatId.toString());
            successMsg.setParseMode(null);
            successMsg.setText("ru".equals(lang) ?
                    "🎉 Поздравляем!\n\n" +
                            "✅ Товар успешно сохранен и мгновенно опубликован в Web и Mini App!\n\n" +
                            "🆔 ID товара: " + product.getId() + "\n" +
                            "🏷️ Название: " + product.getTitleUz() + "\n" +
                            "💰 Цена: " + formatMoney(offer.getPriceUzs()) + " сум" :
                    "🎉 Tabriklaymiz!\n\n" +
                            "✅ Mahsulot muvaffaqiyatli saqlandi va barcha tizimlarda (Web, Mini App) darhol e'lon qilindi!\n\n" +
                            "🆔 Mahsulot ID: " + product.getId() + "\n" +
                            "🏷️ Nomi: " + product.getTitleUz() + "\n" +
                            "💰 Narxi: " + formatMoney(offer.getPriceUzs()) + " so'm");

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
        Optional<Store> storeOpt = findActiveStoreForUser(chatId, session.getPhoneNumber());
        if (storeOpt.isEmpty()) {
            handleStart(chatId, session, null, lang);
            return;
        }

        Store store = storeOpt.get();
        session.setStore(store);

        List<ProductOffer> myOffers = offerRepository.findByStoreId(store.getId());
        if (myOffers.isEmpty()) {
            SendMessage msg = new SendMessage();
            msg.setChatId(chatId.toString());
            msg.setParseMode(null);
            msg.setText("ru".equals(lang) ?
                    "📦 В вашем магазине пока нет товаров.\n\nЧтобы добавить новый товар, нажмите ➕ Добавить товар." :
                    "📦 Do'koningizda hali mahsulotlar mavjud emas.\n\nYangi mahsulot qo'shish uchun ➕ Yangi Mahsulot Qo'shish tugmasini bosing.");
            send(msg);
            return;
        }

        StringBuilder sb = new StringBuilder();
        if ("ru".equals(lang)) {
            sb.append("🏪 Товары магазина ").append(store.getName()).append(" (Всего: ").append(myOffers.size()).append(" шт.):\n\n");
        } else {
            sb.append("🏪 ").append(store.getName()).append(" mahsulotlari (Jami: ").append(myOffers.size()).append(" ta):\n\n");
        }

        int count = 0;
        for (ProductOffer offer : myOffers) {
            count++;
            sb.append(count).append(". ").append(offer.getProduct().getTitleUz()).append("\n")
                    .append("   💰 ").append("ru".equals(lang) ? "Цена: " : "Narxi: ").append(formatMoney(offer.getPriceUzs())).append(" so'm\n\n");
            if (count >= 10) break;
        }

        SendMessage msg = new SendMessage();
        msg.setChatId(chatId.toString());
        msg.setParseMode(null);
        msg.setText(sb.toString());
        send(msg);
    }

    private void handleStartPriceUpdate(Long chatId, SellerSession session, String lang) {
        Optional<Store> storeOpt = findActiveStoreForUser(chatId, session.getPhoneNumber());
        if (storeOpt.isEmpty()) {
            handleStart(chatId, session, null, lang);
            return;
        }

        Store store = storeOpt.get();
        session.setStore(store);

        List<ProductOffer> myOffers = offerRepository.findByStoreId(store.getId());
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
        msg.setParseMode(null);
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
        Optional<Store> storeOpt = findActiveStoreForUser(chatId, session.getPhoneNumber());

        if (prodId != null && storeOpt.isPresent()) {
            Store store = storeOpt.get();
            List<ProductOffer> offers = offerRepository.findByProductId(prodId);
            for (ProductOffer offer : offers) {
                if (offer.getStore().getId().equals(store.getId())) {
                    offer.setOldPriceUzs(offer.getPriceUzs());
                    offer.setPriceUzs(newPrice);
                    offerRepository.save(offer);

                    productRepository.findById(prodId).ifPresent(p -> {
                        priceHistoryRepository.save(new PriceHistory(null, p, newPrice, LocalDateTime.now()));
                    });

                    SendMessage success = new SendMessage();
                    success.setChatId(chatId.toString());
                    success.setParseMode(null);
                    success.setText("ru".equals(lang) ?
                            "✅ Цена успешно обновлена!\n\n💰 Новая цена: " + formatMoney(newPrice) + " сум" :
                            "✅ Narx muvaffaqiyatli yangilandi!\n\n💰 Yangi narx: " + formatMoney(newPrice) + " so'm");
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
            sb.append("🔥 Лучшие предложения сегодня:\n\n");
        } else {
            sb.append("🔥 Bugungi Eng Yaxshi Takliflar:\n\n");
        }

        int count = 0;
        for (ProductDto p : all) {
            count++;
            String title = "ru".equals(lang) ? (p.getTitleRu() != null && !p.getTitleRu().isEmpty() ? p.getTitleRu() : p.getTitleUz()) : p.getTitleUz();
            sb.append(count).append(". 📱 ").append(title).append("\n")
                    .append("   💰 ").append("ru".equals(lang) ? "Цена: " : "Narxi: ").append(formatMoney(p.getLowestPriceUzs())).append(" ").append("ru".equals(lang) ? "сум" : "so'm").append("\n")
                    .append("   🏪 ").append("ru".equals(lang) ? "Магазин: " : "Do'kon: ").append(p.getStoreName()).append("\n\n");
            if (count >= 5) break;
        }

        SendMessage msg = new SendMessage();
        msg.setChatId(chatId.toString());
        msg.setParseMode(null);
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
            msg.setText("ru".equals(lang) ? "⭐ У вас пока нет сохраненных товаров в избранном." : "⭐ Sizda hali saralangan mahsulotlar yo'q.");
            send(msg);
            return;
        }

        List<Favorite> favs = favoriteRepository.findByUserId(userOpt.get().getId());
        if (favs.isEmpty()) {
            SendMessage msg = new SendMessage();
            msg.setChatId(chatId.toString());
            msg.setText("ru".equals(lang) ? "⭐ У вас пока нет сохраненных товаров в избранном." : "⭐ Sizda hali saralangan mahsulotlar yo'q.");
            send(msg);
            return;
        }

        StringBuilder sb = new StringBuilder();
        if ("ru".equals(lang)) {
            sb.append("⭐ Ваши избранные товары:\n\n");
        } else {
            sb.append("⭐ Sizning Sevimli Mahsulotlaringiz:\n\n");
        }

        int count = 0;
        for (Favorite f : favs) {
            count++;
            String title = "ru".equals(lang) ? (f.getProduct().getTitleRu() != null && !f.getProduct().getTitleRu().isEmpty() ? f.getProduct().getTitleRu() : f.getProduct().getTitleUz()) : f.getProduct().getTitleUz();
            sb.append(count).append(". 📱 ").append(title).append("\n\n");
        }

        SendMessage msg = new SendMessage();
        msg.setChatId(chatId.toString());
        msg.setParseMode(null);
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
            sb.append("🔔 Ваши уведомления о ценах:\n\n");
            for (PriceAlert a : alerts) {
                String title = a.getProduct().getTitleRu() != null && !a.getProduct().getTitleRu().isEmpty() ? a.getProduct().getTitleRu() : a.getProduct().getTitleUz();
                sb.append("• ").append(title).append("\n")
                        .append("  🎯 Целевая цена: ").append(formatMoney(a.getTargetPriceUzs())).append(" сум\n\n");
            }
        } else {
            sb.append("🔔 Sizning Narx Alertlaringiz:\n\n");
            for (PriceAlert a : alerts) {
                sb.append("• ").append(a.getProduct().getTitleUz()).append("\n")
                        .append("  🎯 Maqsadli narx: ").append(formatMoney(a.getTargetPriceUzs())).append(" so'm\n\n");
            }
        }

        SendMessage msg = new SendMessage();
        msg.setChatId(chatId.toString());
        msg.setParseMode(null);
        msg.setText(sb.toString());
        send(msg);
    }

    private void handleDefaultSearchOrHelp(Long chatId, String text, String lang) {
        List<ProductDto> localProducts = productService.searchProducts(text);
        List<UzumProductDto> uzumProducts = uzumMarketService.searchProducts(text);

        if (!localProducts.isEmpty() || !uzumProducts.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            boolean isRu = "ru".equals(lang);

            if (isRu) {
                sb.append("🔍 Результаты поиска для \"").append(text).append("\":\n\n");
            } else {
                sb.append("🔍 \"").append(text).append("\" bo'yicha qidiruv natijalari:\n\n");
            }

            int count = 0;
            // 1. Local Database Products
            for (ProductDto top : localProducts) {
                count++;
                String title = isRu ? (top.getTitleRu() != null && !top.getTitleRu().isEmpty() ? top.getTitleRu() : top.getTitleUz()) : top.getTitleUz();
                sb.append(count).append(". 📱 ").append(title).append("\n")
                        .append("   💰 ").append(isRu ? "Цена: " : "Narxi: ").append(formatMoney(top.getLowestPriceUzs())).append(isRu ? " сум" : " so'm").append("\n")
                        .append("   🏪 ").append(isRu ? "Магазин: " : "Do'kon: ").append(top.getStoreName()).append("\n\n");
                if (count >= 3) break;
            }

            // 2. Uzum Market Live API Products
            if (!uzumProducts.isEmpty()) {
                sb.append(isRu ? "🛍️ Из Uzum Market:\n\n" : "🛍️ Uzum Market'dan:\n\n");
                for (UzumProductDto u : uzumProducts) {
                    count++;
                    String title = u.getTitle();
                    Long currentPrice = u.getPrice();
                    Long fullPrice = u.getFullPrice();
                    Double rating = u.getRating() != null ? u.getRating() : 4.8;

                    sb.append(count).append(". 🛍️ ").append(title).append("\n");
                    sb.append("   💰 ").append(isRu ? "Цена: " : "Narxi: ").append(formatMoney(currentPrice)).append(isRu ? " сум" : " so'm");

                    if (fullPrice != null && fullPrice > currentPrice) {
                        long discount = Math.round((1.0 - ((double) currentPrice / fullPrice)) * 100);
                        if (discount > 0) {
                            sb.append(" (<s>").append(formatMoney(fullPrice)).append("</s> -").append(discount).append("%)");
                        }
                    }
                    sb.append("\n   ⭐ ").append(isRu ? "Рейтинг: " : "Reyting: ").append(rating);
                    sb.append("\n   🔗 ").append(u.getProductUrl()).append("\n\n");

                    if (count >= 6) break;
                }
            }

            SendMessage msg = new SendMessage();
            msg.setChatId(chatId.toString());
            msg.setParseMode(null);
            msg.setText(sb.toString());

            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> rows = new ArrayList<>();
            List<InlineKeyboardButton> r = new ArrayList<>();

            if (!localProducts.isEmpty()) {
                InlineKeyboardButton btn = new InlineKeyboardButton();
                btn.setText(isRu ? "📱 Открыть в приложении" : "📱 Mini App'da Ko'rish");
                btn.setWebApp(new WebAppInfo(webappUrl + "/product/" + localProducts.get(0).getId()));
                r.add(btn);
            }
            if (!uzumProducts.isEmpty()) {
                InlineKeyboardButton uzumBtn = new InlineKeyboardButton();
                uzumBtn.setText(isRu ? "🛍️ Открыть в Uzum" : "🛍️ Uzum'da Ko'rish");
                uzumBtn.setUrl(uzumProducts.get(0).getProductUrl());
                r.add(uzumBtn);
            }

            if (!r.isEmpty()) {
                rows.add(r);
                markup.setKeyboard(rows);
                msg.setReplyMarkup(markup);
            }

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
            if (msg.getParseMode() != null) {
                try {
                    msg.setParseMode(null);
                    execute(msg);
                    return;
                } catch (TelegramApiException ignored) {}
            }
            e.printStackTrace();
        }
    }
}
