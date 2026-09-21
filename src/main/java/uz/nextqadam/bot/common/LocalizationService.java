package uz.nextqadam.bot.common;

import java.util.Locale;

import uz.nextqadam.bot.common.enums.Language;

public interface LocalizationService {

    String get(Language lang, String key, Object... args);

    Locale toLocale(Language lang);
}
