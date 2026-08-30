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
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
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
    private final UserService userService;
    private final ProductService productService;

    // In-memory sessions by Chat ID
    private final Map<Long, SellerSession> sessions = new ConcurrentHashMap<>();

    public SellerBotService(ProductRepository productRepository,
                            ProductOfferRepository offerRepository,
                            PriceHistoryRepository priceHistoryRepository,
                            CategoryRepository categoryRepository,
                            StoreRepository storeRepository,
                            UserService userService,
                            ProductService productService) {
        this.productRepository = productRepository;
        this.offerRepository = offerRepository;
        this.priceHistoryRepository = priceHistoryRepository;
        this.categoryRepository = categoryRepository;
        this.storeRepository = storeRepository;
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

        // 1. Check if message is a Contact share
        if (message.hasContact()) {
            handleContactReceived(chatId, message.getContact(), session);
            return;
        }

        // 2. Handle Text Commands & Flow
        if (message.hasText()) {
            String text = message.getText().trim();

            if ("/start".equalsIgnoreCase(text)) {
                handleStart(chatId, session, tgUser);
                return;
            }

            if ("➕ Yangi Mahsulot Qo'shish".equalsIgnoreCase(text)) {
                startAddProductFlow(chatId, session);
                return;
            }

            if ("📦 Mening Mahsulotlarim".equalsIgnoreCase(text)) {
                handleListMyProducts(chatId, session);
                return;
            }

            if ("✏️ Narxni Yangilash".equalsIgnoreCase(text)) {
                handleStartPriceUpdate(chatId, session);
                return;
            }

            if ("📞 Qo'llab-quvvatlash".equalsIgnoreCase(text)) {
                handleSupport(chatId);
                return;
            }

            if ("🏠 Asosiy Menyu".equalsIgnoreCase(text) || "/menu".equalsIgnoreCase(text)) {
                session.setState(SellerState.MAIN_MENU);
                session.clearTempProductData();
                sendMainMenu(chatId, "Asosiy menyuga qaytdingiz:", session);
                return;
            }

            // Route based on FSM state
            switch (session.getState()) {
                case ADD_PRODUCT_NAME -> handleProductNameInput(chatId, text, session);
                case ADD_PRODUCT_PRICE -> handleProductPriceInput(chatId, text, session);
                case ADD_PRODUCT_DESCRIPTION -> handleProductDescriptionInput(chatId, text, session);
                case UPDATE_PRICE_ENTER -> handleProductNewPriceValue(chatId, text, session);
                default -> handleDefaultSearchOrHelp(chatId, text);
            }
            return;
        }

        // 3. Handle Photo Upload for Add Product
        if (message.hasPhoto() && session.getState() == SellerState.ADD_PRODUCT_PHOTO) {
            handleProductPhotoUpload(chatId, message.getPhoto(), session);
        }
    }

    // --- Authentication & Start ---

    private void handleStart(Long chatId, SellerSession session, org.telegram.telegrambots.meta.api.objects.User tgUser) {
        String name = tgUser != null && tgUser.getFirstName() != null ? tgUser.getFirstName() : "Sotuvchi";

        // Check if seller already verified in database
        Optional<Store> existingStore = storeRepository.findByOwnerChatId(chatId);
        if (existingStore.isPresent()) {
            session.setStore(existingStore.get());
            session.setPhoneNumber(existingStore.get().getOwnerPhone());
            session.setState(SellerState.MAIN_MENU);
            sendMainMenu(chatId, "👋 *Xush kelibsiz, " + name + "!*\n\n🏪 Do'koningiz: *" + existingStore.get().getName() + "*\nQuyidagi menyudan kerakli amalni tanlang:", session);
            return;
        }

        // Prompt for Contact
        session.setState(SellerState.AWAITING_CONTACT);
        SendMessage msg = new SendMessage();
        msg.setChatId(chatId.toString());
        msg.setParseMode("Markdown");
        msg.setText("👋 *Assalomu alaykum, " + name + "!*\n\n" +
                "🛒 *PRICEIQ Sotuvchilar va Do'konlar Botiga xush kelibsiz!*\n\n" +
                "Do'koningizni avtomatik tasdiqlash va mahsulotlaringizni boshqarish uchun pastdagi *📱 Telefon Raqamni Yuborish* tugmasini bosing:");

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(true);

        List<KeyboardRow> rows = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        KeyboardButton contactButton = new KeyboardButton("📱 Telefon Raqamni Yuborish");
        contactButton.setRequestContact(true);
        row.add(contactButton);
        rows.add(row);

        keyboardMarkup.setKeyboard(rows);
        msg.setReplyMarkup(keyboardMarkup);

        send(msg);
    }

    private void handleContactReceived(Long chatId, Contact contact, SellerSession session) {
        String phone = contact.getPhoneNumber();
        if (phone == null) phone = "";
        String cleanPhone = phone.replaceAll("[^0-9]", "");

        // Search store by phone
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

            sendMainMenu(chatId, "✅ *Tabriklaymiz, muvaffaqiyatli ulandingiz!*\n\n" +
                    "🏪 *Do'koningiz:* `" + store.getName() + "`\n" +
                    "📞 *Telefon:* `" + phone + "`\n\n" +
                    "Endi to'g'ridan-to'g'ri Telegram orqali yangi mahsulot qo'shishingiz va narxlarni boshqarishingiz mumkin:", session);
        } else {
            SendMessage msg = new SendMessage();
            msg.setChatId(chatId.toString());
            msg.setParseMode("Markdown");
            msg.setText("❌ *Ushbu telefon raqamiga (" + phone + ") biriktirilgan do'kon topilmadi.*\n\n" +
                    "Iltimos, do'koningizni tizimga qo'shish uchun administrator bilan bog'laning: @priceiq_admin");
            send(msg);
        }
    }

    // --- Main Menu ---

    private void sendMainMenu(Long chatId, String text, SellerSession session) {
        SendMessage msg = new SendMessage();
        msg.setChatId(chatId.toString());
        msg.setText(text);
        msg.setParseMode("Markdown");

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);

        List<KeyboardRow> rows = new ArrayList<>();

        KeyboardRow r1 = new KeyboardRow();
        r1.add(new KeyboardButton("➕ Yangi Mahsulot Qo'shish"));
        r1.add(new KeyboardButton("📦 Mening Mahsulotlarim"));

        KeyboardRow r2 = new KeyboardRow();
        r2.add(new KeyboardButton("✏️ Narxni Yangilash"));
        r2.add(new KeyboardButton("📞 Qo'llab-quvvatlash"));

        rows.add(r1);
        rows.add(r2);
        keyboardMarkup.setKeyboard(rows);
        msg.setReplyMarkup(keyboardMarkup);

        send(msg);
    }

    // --- Add Product FSM Flow ---

    private void startAddProductFlow(Long chatId, SellerSession session) {
        if (session.getStore() == null) {
            handleStart(chatId, session, null);
            return;
        }

        session.clearTempProductData();
        session.setState(SellerState.ADD_PRODUCT_PHOTO);

        SendMessage msg = new SendMessage();
        msg.setChatId(chatId.toString());
        msg.setParseMode("Markdown");
        msg.setText("📸 *1-qadam: Mahsulot rasmini yuboring.*\n\n" +
                "Iltimos, mahsulotning aniq va sifatli rasmini chatga yuboring (yoki 'O'tkazib yuborish' deb yozing):");

        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        keyboard.setResizeKeyboard(true);
        List<KeyboardRow> rows = new ArrayList<>();
        KeyboardRow r = new KeyboardRow();
        r.add(new KeyboardButton("🏠 Asosiy Menyu"));
        rows.add(r);
        keyboard.setKeyboard(rows);
        msg.setReplyMarkup(keyboard);

        send(msg);
    }

    private void handleProductPhotoUpload(Long chatId, List<PhotoSize> photos, SellerSession session) {
        // Pick highest resolution photo
        PhotoSize largestPhoto = photos.stream()
                .max(Comparator.comparing(PhotoSize::getFileSize))
                .orElse(photos.get(photos.size() - 1));

        String fileId = largestPhoto.getFileId();
        session.setTempPhotoFileId(fileId);

        // Fetch direct download link from Telegram
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
        msg.setText("✅ Rasm qabul qilindi!\n\n" +
                "📝 *2-qadam: Mahsulotning to'liq nomini kiriting.*\n\n" +
                "Misol: `iPhone 16 Pro Max 256GB` yoki `Samsung 55\" Smart TV` yoki `Artel Kir yuvish mashinasi`:");
        send(msg);
    }

    private void handleProductNameInput(Long chatId, String text, SellerSession session) {
        if ("O'tkazib yuborish".equalsIgnoreCase(text) && session.getTempPhotoUrl() == null) {
            session.setTempPhotoUrl("https://images.unsplash.com/photo-1523275335684-37898b6baf30?auto=format&fit=crop&w=600&q=80");
        }

        session.setTempTitle(text);
        session.setState(SellerState.ADD_PRODUCT_PRICE);

        SendMessage msg = new SendMessage();
        msg.setChatId(chatId.toString());
        msg.setParseMode("Markdown");
        msg.setText("💰 *3-qadam: Mahsulot narxini kiriting.*\n\n" +
                "Misol: `15 000 000` yoki `1200 USD` yoki `8500000`:");
        send(msg);
    }

    private void handleProductPriceInput(Long chatId, String text, SellerSession session) {
        Long priceUzs = parsePrice(text);
        if (priceUzs == null || priceUzs <= 0) {
            SendMessage msg = new SendMessage();
            msg.setChatId(chatId.toString());
            msg.setText("❌ Narx noto'g'ri kiritildi. Iltimos, faqat son kiriting (masalan: `12500000` yoki `1000 USD`):");
            send(msg);
            return;
        }

        session.setTempPriceUzs(priceUzs);
        session.setState(SellerState.ADD_PRODUCT_DESCRIPTION);

        SendMessage msg = new SendMessage();
        msg.setChatId(chatId.toString());
        msg.setParseMode("Markdown");
        msg.setText("📄 *4-qadam: Mahsulot haqida qisqacha tavsif / xususiyatlarini kiriting.*\n\n" +
                "(Agar tavsif kerak bo'lmasa, `Yo'q` deb yozing):");
        send(msg);
    }

    private void handleProductDescriptionInput(Long chatId, String text, SellerSession session) {
        if ("Yo'q".equalsIgnoreCase(text) || "none".equalsIgnoreCase(text) || "-".equals(text)) {
            session.setTempDescription("Yangi mahsulot. Rasmiy kafolat bilan.");
        } else {
            session.setTempDescription(text);
        }

        session.setState(SellerState.ADD_PRODUCT_CONFIRM);

        // Send Preview with Confirmation Inline Buttons
        String caption = "📋 *5-qadam: Mahsulot ma'lumotlarini tasdiqlang:*\n\n" +
                "🏷️ *Nomi:* `" + session.getTempTitle() + "`\n" +
                "🏪 *Do'kon:* `" + session.getStore().getName() + "`\n" +
                "💰 *Narxi:* `" + formatMoney(session.getTempPriceUzs()) + " so'm`\n" +
                "📄 *Tavsif:* " + session.getTempDescription() + "\n\n" +
                "Barcha ma'lumotlar to'g'rimi?";

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        List<InlineKeyboardButton> r = new ArrayList<>();

        InlineKeyboardButton confirmBtn = new InlineKeyboardButton();
        confirmBtn.setText("✅ Tasdiqlash va Saqlash");
        confirmBtn.setCallbackData("confirm_add_product");

        InlineKeyboardButton cancelBtn = new InlineKeyboardButton();
        cancelBtn.setText("❌ Bekor qilish");
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
        SellerSession session = sessions.computeIfAbsent(chatId, id -> new SellerSession(chatId, query.getFrom().getId()));

        if ("confirm_add_product".equals(data)) {
            saveNewProductToDatabase(chatId, session, messageId);
        } else if ("cancel_add_product".equals(data)) {
            session.clearTempProductData();
            session.setState(SellerState.MAIN_MENU);
            sendMainMenu(chatId, "❌ Mahsulot qo'shish bekor qilindi.", session);
        } else if (data.startsWith("update_price_")) {
            Long productId = Long.parseLong(data.replace("update_price_", ""));
            session.setTempSelectedProductId(productId);
            session.setState(SellerState.UPDATE_PRICE_ENTER);

            SendMessage msg = new SendMessage();
            msg.setChatId(chatId.toString());
            msg.setParseMode("Markdown");
            msg.setText("💰 Tanlangan mahsulot uchun *yangi narxni* kiriting (so'mda):");
            send(msg);
        }
    }

    private void saveNewProductToDatabase(Long chatId, SellerSession session, Integer messageId) {
        if (session.getStore() == null || session.getTempTitle() == null || session.getTempPriceUzs() == null) {
            SendMessage msg = new SendMessage();
            msg.setChatId(chatId.toString());
            msg.setText("⚠️ Sessiya ma'lumotlari topilmadi. Qaytadan urinib ko'ring.");
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
            successMsg.setText("🎉 *Tabriklaymiz!*\n\n" +
                    "✅ Mahsulot muvaffaqiyatli saqlandi va web-saytingizda hamda Telegram Mini App'da darhol e'lon qilindi!\n\n" +
                    "🆔 *Mahsulot ID:* `" + product.getId() + "`\n" +
                    "🏷️ *Nomi:* `" + product.getTitleUz() + "`\n" +
                    "💰 *Narxi:* `" + formatMoney(offer.getPriceUzs()) + " so'm`");

            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> rows = new ArrayList<>();
            List<InlineKeyboardButton> r = new ArrayList<>();

            InlineKeyboardButton viewBtn = new InlineKeyboardButton();
            viewBtn.setText("📱 Mini App'da Ko'rish");
            viewBtn.setWebApp(new WebAppInfo(webappUrl + "/product/" + product.getId()));
            r.add(viewBtn);
            rows.add(r);
            markup.setKeyboard(rows);
            successMsg.setReplyMarkup(markup);

            send(successMsg);
            sendMainMenu(chatId, "Asosiy menyu:", session);

        } catch (Exception e) {
            e.printStackTrace();
            SendMessage errMsg = new SendMessage();
            errMsg.setChatId(chatId.toString());
            errMsg.setText("❌ Xatolik yuz berdi: " + e.getMessage());
            send(errMsg);
        }
    }

    // --- Product List & Price Update ---

    private void handleListMyProducts(Long chatId, SellerSession session) {
        if (session.getStore() == null) {
            handleStart(chatId, session, null);
            return;
        }

        List<ProductOffer> myOffers = offerRepository.findByStoreId(session.getStore().getId());
        if (myOffers.isEmpty()) {
            SendMessage msg = new SendMessage();
            msg.setChatId(chatId.toString());
            msg.setParseMode("Markdown");
            msg.setText("📦 *Do'koningizda hali mahsulotlar mavjud emas.*\n\nYangi mahsulot qo'shish uchun *➕ Yangi Mahsulot Qo'shish* tugmasini bosing.");
            send(msg);
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("🏪 *").append(session.getStore().getName()).append("* mahsulotlari (Jami: ").append(myOffers.size()).append(" ta):\n\n");

        int count = 0;
        for (ProductOffer offer : myOffers) {
            count++;
            sb.append(count).append(". *").append(offer.getProduct().getTitleUz()).append("*\n")
                    .append("   💰 Narxi: `").append(formatMoney(offer.getPriceUzs())).append(" so'm`\n\n");
            if (count >= 10) break;
        }

        SendMessage msg = new SendMessage();
        msg.setChatId(chatId.toString());
        msg.setParseMode("Markdown");
        msg.setText(sb.toString());
        send(msg);
    }

    private void handleStartPriceUpdate(Long chatId, SellerSession session) {
        if (session.getStore() == null) {
            handleStart(chatId, session, null);
            return;
        }

        List<ProductOffer> myOffers = offerRepository.findByStoreId(session.getStore().getId());
        if (myOffers.isEmpty()) {
            SendMessage msg = new SendMessage();
            msg.setChatId(chatId.toString());
            msg.setText("📦 Narxini o'zgartirish uchun mahsulotlar topilmadi.");
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
        msg.setText("✏️ Narxini yangilamoqchi bo'lgan mahsulotni tanlang:");
        msg.setReplyMarkup(markup);
        send(msg);
    }

    private void handleProductNewPriceValue(Long chatId, String text, SellerSession session) {
        Long newPrice = parsePrice(text);
        if (newPrice == null || newPrice <= 0) {
            SendMessage msg = new SendMessage();
            msg.setChatId(chatId.toString());
            msg.setText("❌ Noto'g'ri narx. Iltimos, faqat son kiriting:");
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
                    success.setText("✅ *Narx muvaffaqiyatli yangilandi!*\n\n💰 Yangi narx: `" + formatMoney(newPrice) + " so'm`");
                    send(success);
                    break;
                }
            }
        }

        session.clearTempProductData();
        session.setState(SellerState.MAIN_MENU);
        sendMainMenu(chatId, "Asosiy menyu:", session);
    }

    private void handleSupport(Long chatId) {
        SendMessage msg = new SendMessage();
        msg.setChatId(chatId.toString());
        msg.setParseMode("Markdown");
        msg.setText("📞 *PRICEIQ Qo'llab-quvvatlash xizmati*\n\n" +
                "Savollar, takliflar yoki muammolar bo'yicha biz bilan bog'laning:\n" +
                "👤 Telegram: @priceiq_admin\n" +
                "📞 Telefon: +998 71 200 00 00\n" +
                "🌐 Veb-sayt: [priceiq.uz](https://frontend-three-gamma-ca7l713sls.vercel.app)");
        send(msg);
    }

    private void handleDefaultSearchOrHelp(Long chatId, String text) {
        List<ProductDto> products = productService.searchProducts(text);
        if (!products.isEmpty()) {
            ProductDto top = products.get(0);
            SendMessage msg = new SendMessage();
            msg.setChatId(chatId.toString());
            msg.setParseMode("Markdown");
            msg.setText("🔍 *Qidiruv natijasi:*\n\n" +
                    "📱 *" + top.getTitleUz() + "*\n" +
                    "💰 Eng arzon narx: `" + formatMoney(top.getLowestPriceUzs()) + " so'm`\n" +
                    "🏪 Do'kon: `" + top.getStoreName() + "`");

            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> rows = new ArrayList<>();
            List<InlineKeyboardButton> r = new ArrayList<>();
            InlineKeyboardButton btn = new InlineKeyboardButton();
            btn.setText("📱 Mini App'da Ko'rish");
            btn.setWebApp(new WebAppInfo(webappUrl + "/product/" + top.getId()));
            r.add(btn);
            rows.add(r);
            markup.setKeyboard(rows);
            msg.setReplyMarkup(markup);

            send(msg);
        } else {
            SendMessage msg = new SendMessage();
            msg.setChatId(chatId.toString());
            msg.setText("Buyruqni tanlash uchun /menu bosing yoki menyu tugmalaridan foydalaning.");
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
                return (long) (usd * 12800); // Current USD/UZS rate
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
