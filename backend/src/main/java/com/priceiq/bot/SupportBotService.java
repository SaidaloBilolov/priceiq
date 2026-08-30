package com.priceiq.bot;

import com.priceiq.entity.SupportOperator;
import com.priceiq.entity.User;
import com.priceiq.repository.SupportOperatorRepository;
import com.priceiq.repository.UserRepository;
import com.priceiq.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.*;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class SupportBotService extends TelegramLongPollingBot {

    @Value("${telegram.support-bot.username:WearFlow_Support_Bot}")
    private String supportBotUsername;

    @Value("${telegram.support-bot.token:${telegram.bot.token}}")
    private String supportBotToken;

    @Value("${telegram.support-bot.admin-chat-id:99887766}")
    private String adminChatId;

    private final UserRepository userRepository;
    private final UserService userService;
    private final SupportOperatorRepository supportOperatorRepository;

    // Mapping of Operator/Admin Chat message ID -> User Chat ID for replies
    private final Map<Integer, Long> operatorMsgToUserChatMap = new ConcurrentHashMap<>();

    // User language cache
    private final Map<Long, String> userLanguageMap = new ConcurrentHashMap<>();

    private static final Pattern CHAT_ID_PATTERN = Pattern.compile("(?:Chat ID|User ID):\\s*(\\d+)", Pattern.CASE_INSENSITIVE);

    public SupportBotService(UserRepository userRepository,
                             UserService userService,
                             SupportOperatorRepository supportOperatorRepository) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.supportOperatorRepository = supportOperatorRepository;
    }

    @Override
    public String getBotUsername() {
        return supportBotUsername;
    }

    @Override
    public String getBotToken() {
        return supportBotToken;
    }

    @Override
    public void onUpdateReceived(Update update) {
        // 1. Handle Language Selection Callback
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

        // 2. Check if message is from an Operator or Admin responding to a user query
        Optional<SupportOperator> operatorOpt = supportOperatorRepository.findByTelegramChatId(chatId);
        if (operatorOpt.isPresent() || isAdminChat(chatId)) {
            if (message.getReplyToMessage() != null) {
                handleOperatorReply(message, operatorOpt.orElse(null));
                return;
            }
        }

        // 3. Handle Contact Sharing (Check for Operator Authorization or User Phone Sync)
        if (message.hasContact()) {
            handleContactReceived(chatId, message.getContact(), tgUser);
            return;
        }

        // 4. User Flow
        if (tgUser != null) {
            userService.getOrCreateUser(tgUser.getId(), tgUser.getFirstName(), tgUser.getUserName(), tgUser.getLanguageCode());
        }

        String userLang = getUserLanguage(chatId, tgUser);

        // Commands
        if (message.hasText()) {
            String text = message.getText().trim();
            if ("/start".equalsIgnoreCase(text) || "/language".equalsIgnoreCase(text) || "/til".equalsIgnoreCase(text)) {
                sendLanguageSelection(chatId);
                return;
            }
            if ("/help".equalsIgnoreCase(text) || "/yordam".equalsIgnoreCase(text)) {
                sendHelpMessage(chatId, userLang);
                return;
            }
        }

        // Forward user message / media to ALL active Support Operators and Admin
        forwardUserInquiryToOperators(message, tgUser, userLang);
    }

    // --- Language & Operator Authorization ---

    private void handleContactReceived(Long chatId, Contact contact, org.telegram.telegrambots.meta.api.objects.User tgUser) {
        String phone = contact.getPhoneNumber();
        if (phone == null) phone = "";
        String cleanPhone = phone.replaceAll("[^0-9]", "");

        // Check if phone number belongs to a Support Operator
        Optional<SupportOperator> opOpt = supportOperatorRepository.findByCleanPhone(cleanPhone);
        if (opOpt.isEmpty()) {
            opOpt = supportOperatorRepository.findByPhoneNumber("+" + cleanPhone);
        }
        if (opOpt.isEmpty()) {
            opOpt = supportOperatorRepository.findByPhoneNumber(phone);
        }

        if (opOpt.isPresent()) {
            SupportOperator op = opOpt.get();
            op.setTelegramChatId(chatId);
            op.setIsActive(true);
            supportOperatorRepository.save(op);

            SendMessage msg = new SendMessage();
            msg.setChatId(chatId.toString());
            msg.setParseMode("Markdown");
            msg.setText("🎧 *Siz Support Operator sifatida tizimga kirdingiz!*\n\n" +
                    "👤 *Operator:* `" + op.getFullName() + "`\n" +
                    "📞 *Telefon:* `" + op.getPhoneNumber() + "`\n\n" +
                    "Foydalanuvchilardan kelgan murojaatlar ushbu chatga keladi. Javob berish uchun xabarga *'Reply' (Javob berish)* qiling.");
            send(msg);
            return;
        }

        // Regular user phone sync
        if (tgUser != null) {
            userService.updatePhoneNumber(tgUser.getId(), phone.startsWith("+") ? phone : "+" + phone, "uz");
        }

        SendMessage msg = new SendMessage();
        msg.setChatId(chatId.toString());
        msg.setText("✅ Telefon raqamingiz qabul qilindi. Savolingizni yozib qoldirishingiz mumkin:");
        send(msg);
    }

    private void sendLanguageSelection(Long chatId) {
        SendMessage msg = new SendMessage();
        msg.setChatId(chatId.toString());
        msg.setText("Tilni tanlang / Выберите язык:");

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        List<InlineKeyboardButton> row = new ArrayList<>();

        InlineKeyboardButton uzBtn = new InlineKeyboardButton();
        uzBtn.setText("🇺🇿 O'zbekcha");
        uzBtn.setCallbackData("set_lang_uz");

        InlineKeyboardButton ruBtn = new InlineKeyboardButton();
        ruBtn.setText("🇷🇺 Русский");
        ruBtn.setCallbackData("set_lang_ru");

        row.add(uzBtn);
        row.add(ruBtn);
        rows.add(row);
        markup.setKeyboard(rows);

        msg.setReplyMarkup(markup);
        send(msg);
    }

    private void handleCallbackQuery(CallbackQuery query) {
        String data = query.getData();
        Long chatId = query.getMessage().getChatId();

        if ("set_lang_uz".equals(data)) {
            setUserLanguage(chatId, query.getFrom().getId(), "uz");
            sendWelcome(chatId, "uz");
        } else if ("set_lang_ru".equals(data)) {
            setUserLanguage(chatId, query.getFrom().getId(), "ru");
            sendWelcome(chatId, "ru");
        }
    }

    private void setUserLanguage(Long chatId, Long telegramId, String lang) {
        userLanguageMap.put(chatId, lang);
        userRepository.findByTelegramId(telegramId).ifPresent(user -> {
            user.setLanguageCode(lang);
            userRepository.save(user);
        });
    }

    private String getUserLanguage(Long chatId, org.telegram.telegrambots.meta.api.objects.User tgUser) {
        if (userLanguageMap.containsKey(chatId)) {
            return userLanguageMap.get(chatId);
        }
        if (tgUser != null) {
            Optional<User> userOpt = userRepository.findByTelegramId(tgUser.getId());
            if (userOpt.isPresent() && userOpt.get().getLanguageCode() != null) {
                String lang = userOpt.get().getLanguageCode();
                userLanguageMap.put(chatId, lang);
                return lang;
            }
            if (tgUser.getLanguageCode() != null && tgUser.getLanguageCode().startsWith("ru")) {
                userLanguageMap.put(chatId, "ru");
                return "ru";
            }
        }
        return "uz";
    }

    private void sendWelcome(Long chatId, String lang) {
        SendMessage msg = new SendMessage();
        msg.setChatId(chatId.toString());
        msg.setParseMode("Markdown");

        if ("ru".equals(lang)) {
            msg.setText("👋 *Здравствуйте!*\n\n" +
                    "Добро пожаловать в службу поддержки *PRICEIQ / WearFlow*.\n\n" +
                    "Опишите ваш вопрос или проблему прямо в этом чате. Наши операторы ответят вам в ближайшее время.\n\n" +
                    "💡 *Рекомендация:* Чтобы мы быстрее разобрались в ситуации, вы можете отправить скриншот, фото или видео (это не обязательно).");
        } else {
            msg.setText("👋 *Assalomu alaykum!*\n\n" +
                    "*PRICEIQ / WearFlow* qo'llab-quvvatlash xizmatiga xush kelibsiz.\n\n" +
                    "Savolingiz yoki murojaatingizni to'g'ridan-to'g'ri shu chatga yozib qoldiring. Operatorlarimiz tez orada sizga javob berishadi.\n\n" +
                    "💡 *Tavsiya:* Masalani tezroq hal qilish uchun skrinshot, rasm yoki video yuborishingiz mumkin (bu majburiy emas).");
        }
        send(msg);
    }

    private void sendHelpMessage(Long chatId, String lang) {
        sendWelcome(chatId, lang);
    }

    // --- Dynamic Multi-Media Forwarding to Operators ---

    private void forwardUserInquiryToOperators(Message message, org.telegram.telegrambots.meta.api.objects.User tgUser, String userLang) {
        Long userChatId = message.getChatId();
        String name = tgUser != null ? tgUser.getFirstName() : "Foydalanuvchi";
        String username = tgUser != null && tgUser.getUserName() != null ? "@" + tgUser.getUserName() : "mavjud emas";
        String phone = getUserPhone(tgUser);

        String header = "📩 *Yangi murojaat! / Новое обращение!*\n" +
                "👤 *Foydalanuvchi:* " + name + " (" + username + ")\n" +
                "🆔 *Chat ID:* `" + userChatId + "`\n" +
                "📞 *Telefon:* `" + phone + "`\n";

        // Collect all active operator chat IDs + Admin Chat ID
        Set<String> recipientChatIds = new HashSet<>();
        if (adminChatId != null && !adminChatId.trim().isEmpty()) {
            recipientChatIds.add(adminChatId.trim());
        }

        List<SupportOperator> activeOps = supportOperatorRepository.findByIsActiveTrue();
        for (SupportOperator op : activeOps) {
            if (op.getTelegramChatId() != null) {
                recipientChatIds.add(op.getTelegramChatId().toString());
            }
        }

        for (String targetChat : recipientChatIds) {
            try {
                // 1. Text Message
                if (message.hasText()) {
                    String fullText = header + "💬 *Xabar:* " + message.getText();
                    SendMessage toOp = new SendMessage();
                    toOp.setChatId(targetChat);
                    toOp.setText(fullText);
                    toOp.setParseMode("Markdown");
                    Message sent = execute(toOp);
                    if (sent != null) {
                        operatorMsgToUserChatMap.put(sent.getMessageId(), userChatId);
                    }
                }
                // 2. Photo Message
                else if (message.hasPhoto()) {
                    List<PhotoSize> photos = message.getPhoto();
                    String fileId = photos.get(photos.size() - 1).getFileId();
                    String caption = header + (message.getCaption() != null ? "💬 *Izoh:* " + message.getCaption() : "");

                    SendPhoto toOp = new SendPhoto();
                    toOp.setChatId(targetChat);
                    toOp.setPhoto(new InputFile(fileId));
                    toOp.setCaption(caption);
                    toOp.setParseMode("Markdown");
                    Message sent = execute(toOp);
                    if (sent != null) {
                        operatorMsgToUserChatMap.put(sent.getMessageId(), userChatId);
                    }
                }
                // 3. Video Message
                else if (message.hasVideo()) {
                    String fileId = message.getVideo().getFileId();
                    String caption = header + (message.getCaption() != null ? "💬 *Izoh:* " + message.getCaption() : "");

                    SendVideo toOp = new SendVideo();
                    toOp.setChatId(targetChat);
                    toOp.setVideo(new InputFile(fileId));
                    toOp.setCaption(caption);
                    toOp.setParseMode("Markdown");
                    Message sent = execute(toOp);
                    if (sent != null) {
                        operatorMsgToUserChatMap.put(sent.getMessageId(), userChatId);
                    }
                }
                // 4. Video Note (Kruglyash)
                else if (message.hasVideoNote()) {
                    String fileId = message.getVideoNote().getFileId();

                    SendVideoNote toOp = new SendVideoNote();
                    toOp.setChatId(targetChat);
                    toOp.setVideoNote(new InputFile(fileId));
                    Message sent = execute(toOp);
                    if (sent != null) {
                        operatorMsgToUserChatMap.put(sent.getMessageId(), userChatId);
                    }

                    SendMessage meta = new SendMessage();
                    meta.setChatId(targetChat);
                    meta.setText(header + "📹 *Video xabar (kruglyash) yuborildi.*");
                    meta.setParseMode("Markdown");
                    Message metaSent = execute(meta);
                    if (metaSent != null) {
                        operatorMsgToUserChatMap.put(metaSent.getMessageId(), userChatId);
                    }
                }
                // 5. Document Message
                else if (message.hasDocument()) {
                    String fileId = message.getDocument().getFileId();
                    String caption = header + (message.getCaption() != null ? "💬 *Izoh:* " + message.getCaption() : "");

                    SendDocument toOp = new SendDocument();
                    toOp.setChatId(targetChat);
                    toOp.setDocument(new InputFile(fileId));
                    toOp.setCaption(caption);
                    toOp.setParseMode("Markdown");
                    Message sent = execute(toOp);
                    if (sent != null) {
                        operatorMsgToUserChatMap.put(sent.getMessageId(), userChatId);
                    }
                }
                // 6. Voice Message
                else if (message.hasVoice()) {
                    String fileId = message.getVoice().getFileId();

                    SendVoice toOp = new SendVoice();
                    toOp.setChatId(targetChat);
                    toOp.setVoice(new InputFile(fileId));
                    toOp.setCaption(header + (message.getCaption() != null ? "💬 *Izoh:* " + message.getCaption() : ""));
                    toOp.setParseMode("Markdown");
                    Message sent = execute(toOp);
                    if (sent != null) {
                        operatorMsgToUserChatMap.put(sent.getMessageId(), userChatId);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Soft recommendation notice to user
        if (message.hasText()) {
            sendSoftRecommendationNotice(userChatId, userLang);
        } else {
            sendMediaReceivedNotice(userChatId, userLang);
        }
    }

    // --- Soft Recommendation Notice ---

    private void sendSoftRecommendationNotice(Long chatId, String lang) {
        SendMessage msg = new SendMessage();
        msg.setChatId(chatId.toString());
        msg.setParseMode("Markdown");

        if ("ru".equals(lang)) {
            msg.setText("✅ *Ваше обращение принято!*\n\n" +
                    "💡 *Рекомендация:* Чтобы мы быстрее разобрались в проблеме, вы можете отправить скриншот или короткое видео (это не обязательно).");
        } else {
            msg.setText("✅ *Murojaatingiz qabul qilindi!*\n\n" +
                    "💡 *Tavsiya:* Muammoni aniqroq tushunishimiz uchun skrinshot yoki qisqa video yuborishingiz mumkin (bu majburiy emas).");
        }
        send(msg);
    }

    private void sendMediaReceivedNotice(Long chatId, String lang) {
        SendMessage msg = new SendMessage();
        msg.setChatId(chatId.toString());
        msg.setParseMode("Markdown");

        if ("ru".equals(lang)) {
            msg.setText("✅ *Ваш файл принят и передан операторам.* Скоро мы вам ответим!");
        } else {
            msg.setText("✅ *Faylingiz qabul qilindi va operatorlarga yetkazildi.* Tez orada javob qaytaramiz!");
        }
        send(msg);
    }

    // --- Operator Reply Routing (Operator -> User) ---

    private void handleOperatorReply(Message message, SupportOperator operator) {
        Message replyTo = message.getReplyToMessage();
        if (replyTo == null) return;

        Long targetUserChatId = extractTargetChatId(replyTo);
        if (targetUserChatId == null) {
            SendMessage warn = new SendMessage();
            warn.setChatId(message.getChatId().toString());
            warn.setText("⚠️ Foydalanuvchi Chat ID si aniqlanmadi. Iltimos, xabarga to'g'ridan-to'g'ri 'Reply' qiling.");
            send(warn);
            return;
        }

        String opName = operator != null ? operator.getFullName() : "Administrator";
        String userLang = userLanguageMap.getOrDefault(targetUserChatId, "uz");
        String prefix = "ru".equals(userLang) ?
                "👨‍💻 *Ответ оператора (" + opName + "):*\n\n" :
                "👨‍💻 *Qo'llab-quvvatlash xizmati javobi (" + opName + "):*\n\n";

        // Forward Operator Response to User
        if (message.hasText()) {
            SendMessage toUser = new SendMessage();
            toUser.setChatId(targetUserChatId.toString());
            toUser.setText(prefix + message.getText());
            toUser.setParseMode("Markdown");
            send(toUser);
        } else if (message.hasPhoto()) {
            String fileId = message.getPhoto().get(message.getPhoto().size() - 1).getFileId();
            SendPhoto toUser = new SendPhoto();
            toUser.setChatId(targetUserChatId.toString());
            toUser.setPhoto(new InputFile(fileId));
            toUser.setCaption(prefix + (message.getCaption() != null ? message.getCaption() : ""));
            toUser.setParseMode("Markdown");
            try { execute(toUser); } catch (Exception e) {}
        } else if (message.hasDocument()) {
            String fileId = message.getDocument().getFileId();
            SendDocument toUser = new SendDocument();
            toUser.setChatId(targetUserChatId.toString());
            toUser.setDocument(new InputFile(fileId));
            toUser.setCaption(prefix + (message.getCaption() != null ? message.getCaption() : ""));
            toUser.setParseMode("Markdown");
            try { execute(toUser); } catch (Exception e) {}
        } else if (message.hasVoice()) {
            String fileId = message.getVoice().getFileId();
            SendVoice toUser = new SendVoice();
            toUser.setChatId(targetUserChatId.toString());
            toUser.setVoice(new InputFile(fileId));
            toUser.setCaption(prefix);
            try { execute(toUser); } catch (Exception e) {}
        } else if (message.hasVideo()) {
            String fileId = message.getVideo().getFileId();
            SendVideo toUser = new SendVideo();
            toUser.setChatId(targetUserChatId.toString());
            toUser.setVideo(new InputFile(fileId));
            toUser.setCaption(prefix + (message.getCaption() != null ? message.getCaption() : ""));
            try { execute(toUser); } catch (Exception e) {}
        }

        // Confirmation to Operator
        SendMessage opConfirm = new SendMessage();
        opConfirm.setChatId(message.getChatId().toString());
        opConfirm.setText("✅ Javobingiz foydalanuvchiga (Chat ID: " + targetUserChatId + ") yetkazildi!");
        send(opConfirm);
    }

    private Long extractTargetChatId(Message replyTo) {
        if (operatorMsgToUserChatMap.containsKey(replyTo.getMessageId())) {
            return operatorMsgToUserChatMap.get(replyTo.getMessageId());
        }

        String text = replyTo.hasText() ? replyTo.getText() : (replyTo.getCaption() != null ? replyTo.getCaption() : "");
        Matcher matcher = CHAT_ID_PATTERN.matcher(text);
        if (matcher.find()) {
            try {
                return Long.parseLong(matcher.group(1));
            } catch (NumberFormatException e) {}
        }

        return null;
    }

    private boolean isAdminChat(Long chatId) {
        if (chatId == null || adminChatId == null) return false;
        return chatId.toString().equals(adminChatId.trim());
    }

    private String getUserPhone(org.telegram.telegrambots.meta.api.objects.User tgUser) {
        if (tgUser == null) return "mavjud emas";
        Optional<User> user = userRepository.findByTelegramId(tgUser.getId());
        return user.map(User::getPhoneNumber).orElse("kiritilmagan");
    }

    private Message send(SendMessage msg) {
        try {
            return execute(msg);
        } catch (TelegramApiException e) {
            e.printStackTrace();
            return null;
        }
    }
}
