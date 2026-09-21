package uz.nextqadam.bot.common.enums;

public enum Language {

    UZ("🇺🇿 O'zbekcha", "uz"),
    RU("🇷🇺 Русский", "ru"),
    EN("🇬🇧 English", "en");

    private final String displayName;
    private final String telegramLocale;

    Language(String displayName, String telegramLocale) {
        this.displayName = displayName;
        this.telegramLocale = telegramLocale;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getTelegramLocale() {
        return telegramLocale;
    }
}