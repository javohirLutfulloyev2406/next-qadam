package uz.nextqadam.bot.common;

import uz.nextqadam.bot.common.enums.ToneType;

public interface MessageTemplateService {

    String welcomeAfterTone(ToneType tone, String userName);

    String taskDoneCongrats(ToneType tone, String taskTitle);

    String noPendingTask(ToneType tone);

    String goalDecompositionIntro(ToneType tone);
}
