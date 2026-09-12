package uz.nextqadam.bot.user;

import java.util.Optional;
import java.util.UUID;

import uz.nextqadam.bot.common.enums.ToneType;

public interface UserService {

    User registerOrGetUser(Long telegramId, String name);

    Optional<User> findByTelegramId(Long telegramId);

    User updateTonePreference(UUID userId, ToneType tonePreference);

    User updateTimezone(UUID userId, String timezone);
}