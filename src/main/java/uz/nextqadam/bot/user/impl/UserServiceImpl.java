package uz.nextqadam.bot.user.impl;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import uz.nextqadam.bot.common.enums.ToneType;
import uz.nextqadam.bot.user.User;
import uz.nextqadam.bot.user.UserRepository;
import uz.nextqadam.bot.user.UserService;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public User registerOrGetUser(Long telegramId, String name) {
        // TODO: implementatsiya
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public Optional<User> findByTelegramId(Long telegramId) {
        // TODO: implementatsiya
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public User updateTonePreference(UUID userId, ToneType tonePreference) {
        // TODO: implementatsiya
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public User updateTimezone(UUID userId, String timezone) {
        // TODO: implementatsiya
        throw new UnsupportedOperationException("Not implemented yet");
    }
}