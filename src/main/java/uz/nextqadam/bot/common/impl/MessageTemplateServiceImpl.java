package uz.nextqadam.bot.common.impl;

import org.springframework.stereotype.Service;

import uz.nextqadam.bot.common.LocalizationService;
import uz.nextqadam.bot.common.MessageTemplateService;
import uz.nextqadam.bot.common.enums.Language;
import uz.nextqadam.bot.common.enums.ToneType;

@Service
public class MessageTemplateServiceImpl implements MessageTemplateService {

    private final LocalizationService localizationService;

    public MessageTemplateServiceImpl(LocalizationService localizationService) {
        this.localizationService = localizationService;
    }

    @Override
    public String welcomeAfterTone(Language language, ToneType tone, String userName) {
        return localizationService.get(language, "onboarding.welcome_after_tone." + tone.name(), userName);
    }

    @Override
    public String taskDoneCongrats(Language language, ToneType tone, String taskTitle) {
        return localizationService.get(language, "task.done." + tone.name(), taskTitle);
    }

    @Override
    public String noPendingTask(Language language, ToneType tone) {
        return localizationService.get(language, "task.none." + tone.name());
    }

    @Override
    public String goalDecompositionIntro(Language language, ToneType tone) {
        return localizationService.get(language, "goal.decomposition_intro." + tone.name());
    }

    @Override
    public String reminderNudge(Language language, ToneType tone, String taskTitle) {
        return localizationService.get(language, "nudge.reminder." + tone.name(), taskTitle);
    }

    @Override
    public String snoozeAck(Language language, ToneType tone) {
        return localizationService.get(language, "nudge.snooze_ack." + tone.name());
    }

    @Override
    public String adaptiveShrinkNotice(Language language, ToneType tone, int newEstimatedMinutes) {
        return localizationService.get(language, "nudge.adaptive_shrink." + tone.name(), newEstimatedMinutes);
    }

    @Override
    public String typingPlaceholder(Language language, ToneType tone) {
        return localizationService.get(language, "common.typing." + tone.name());
    }
}
