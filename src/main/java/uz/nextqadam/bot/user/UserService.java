package uz.nextqadam.bot.user;

import java.util.Optional;
import java.util.UUID;

import uz.nextqadam.bot.common.enums.Language;
import uz.nextqadam.bot.common.enums.ToneType;

public interface UserService {

    Optional<User> findByTelegramId(Long telegramId);

    /**
     * AdminHandler'ga kerak — qidiruv natijasidan keyingi kontekstli callback'larda (masalan
     * faoliyat tarixini ko'rish) qo'lda faqat UUID mavjud bo'ladi, Telegram ID emas.
     */
    Optional<User> findById(UUID userId);

    User createUser(Long telegramId, String name);

    /**
     * Onboarding'ning YANGI birinchi bosqichi — til tanlangach, ism hali so'ralmagan bo'lsa ham
     * User qatorini oldindan yaratadi (name=null), shu bilan keyingi bosqichlar (ism, ton) bir xil
     * "mavjud userni yangilash" yo'lidan o'tadi.
     */
    User createUserWithLanguage(Long telegramId, Language language);

    User updateName(UUID userId, String name);

    User updateTonePreference(UUID userId, ToneType tone);

    User updateTimezone(UUID userId, String timezone);

    User updateLanguage(UUID userId, Language language);
}
