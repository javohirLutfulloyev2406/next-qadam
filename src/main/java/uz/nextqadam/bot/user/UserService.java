package uz.nextqadam.bot.user;

import java.util.Optional;
import java.util.UUID;

import uz.nextqadam.bot.common.enums.ToneType;

public interface UserService {

    Optional<User> findByTelegramId(Long telegramId);

    User createUser(Long telegramId, String name);

    User updateName(UUID userId, String name);

    User updateTonePreference(UUID userId, ToneType tone);

    User updateTimezone(UUID userId, String timezone);
}
