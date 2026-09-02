package com.priceiq.service;

import com.priceiq.dto.UpdatePhoneRequest;
import com.priceiq.entity.User;
import com.priceiq.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public User getOrCreateUser(Long telegramId, String firstName, String username, String languageCode) {
        return userRepository.findByTelegramId(telegramId)
                .map(user -> {
                    boolean updated = false;
                    if (firstName != null && !firstName.equals(user.getFirstName())) {
                        user.setFirstName(firstName);
                        updated = true;
                    }
                    if (username != null && !username.equals(user.getUsername())) {
                        user.setUsername(username);
                        updated = true;
                    }
                    if (user.getLanguageCode() == null && languageCode != null) {
                        String norm = languageCode.toLowerCase().startsWith("ru") ? "ru" : "uz";
                        user.setLanguageCode(norm);
                        updated = true;
                    }
                    return updated ? userRepository.save(user) : user;
                })
                .orElseGet(() -> {
                    User u = new User();
                    u.setTelegramId(telegramId);
                    u.setFirstName(firstName);
                    u.setUsername(username);
                    String norm = (languageCode != null && languageCode.toLowerCase().startsWith("ru")) ? "ru" : "uz";
                    u.setLanguageCode(norm);
                    return userRepository.save(u);
                });
    }

    @Transactional
    public User updatePhoneNumber(Long telegramId, String phoneNumber, String languageCode) {
        UpdatePhoneRequest req = new UpdatePhoneRequest();
        req.setTelegramId(telegramId);
        req.setPhoneNumber(phoneNumber);
        req.setLanguageCode(languageCode);
        return updatePhoneNumber(req);
    }

    @Transactional
    public User updatePhoneNumber(UpdatePhoneRequest request) {
        User user = userRepository.findByTelegramId(request.getTelegramId())
                .orElseGet(() -> {
                    User u = new User();
                    u.setTelegramId(request.getTelegramId());
                    u.setLanguageCode(request.getLanguageCode() != null ? request.getLanguageCode() : "uz");
                    return u;
                });

        user.setPhoneNumber(request.getPhoneNumber());
        if (request.getLanguageCode() != null) {
            user.setLanguageCode(request.getLanguageCode());
        }
        return userRepository.save(user);
    }

    public Optional<User> findByTelegramId(Long telegramId) {
        return userRepository.findByTelegramId(telegramId);
    }
}
