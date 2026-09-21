package uz.nextqadam.bot.common.impl;

import java.util.Locale;

import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

import uz.nextqadam.bot.common.LocalizationService;
import uz.nextqadam.bot.common.enums.Language;

@Service
public class LocalizationServiceImpl implements LocalizationService {

    private final MessageSource messageSource;

    public LocalizationServiceImpl(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @Override
    public String get(Language lang, String key, Object... args) {
        return messageSource.getMessage(key, args, toLocale(lang));
    }

    @Override
    public Locale toLocale(Language lang) {
        return Locale.of(lang.getTelegramLocale());
    }
}
