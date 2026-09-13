package uz.nextqadam.bot.user.impl;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import uz.nextqadam.bot.common.enums.ToneType;
import uz.nextqadam.bot.common.exception.NextQadamException;
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
    public Optional<User> findByTelegramId(Long telegramId) {
        return userRepository.findByTelegramId(telegramId);
    }

    @Override
    public User createUser(Long telegramId, String name) {
        User user = User.builder()
                .telegramId(telegramId)
                .name(name)
                .tonePreference(ToneType.NORMAL)
                .build();
        return userRepository.save(user);
    }

    @Override
    public User updateTonePreference(UUID userId, ToneType tone) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NextQadamException("Foydalanuvchi topilmadi: " + userId));
        user.setTonePreference(tone);
        return userRepository.save(user);
    }

    @Override
    public User updateTimezone(UUID userId, String timezone) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NextQadamException("Foydalanuvchi topilmadi: " + userId));
        user.setTimezone(timezone);
        return userRepository.save(user);
    }
}