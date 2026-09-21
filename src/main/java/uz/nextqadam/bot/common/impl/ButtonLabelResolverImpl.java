package uz.nextqadam.bot.common.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;

import uz.nextqadam.bot.common.BotCommand;
import uz.nextqadam.bot.common.ButtonLabelResolver;
import uz.nextqadam.bot.common.LocalizationService;
import uz.nextqadam.bot.common.enums.Language;

@Service
public class ButtonLabelResolverImpl implements ButtonLabelResolver {

    private final Map<String, BotCommand> labelToCommand;

    public ButtonLabelResolverImpl(LocalizationService localizationService) {
        Map<String, BotCommand> map = new HashMap<>();
        for (BotCommand command : BotCommand.values()) {
            if (command.getMenuKey() == null) {
                continue;
            }
            for (Language language : Language.values()) {
                map.put(localizationService.get(language, command.getMenuKey()), command);
            }
        }
        this.labelToCommand = Map.copyOf(map);
    }

    @Override
    public Optional<BotCommand> resolve(String label) {
        if (label == null || label.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(labelToCommand.get(label.trim()));
    }
}
