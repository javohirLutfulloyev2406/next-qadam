package uz.nextqadam.bot.common;

import java.util.Optional;

/**
 * Doimiy tugmalar panelidagi (ReplyKeyboard) matnni tegishli BotCommand'ga bog'laydi. Tugma matnlari
 * endi tilga qarab farq qiladi (KeyboardService.buildMainMenuKeyboard), shu sababli BotCommand ichidagi
 * eski hardcoded o'zbekcha xarita (fromButtonLabel) yetarli emas — bu interfeys barcha Language'lar
 * bo'yicha lokalizatsiya qilingan matnlarni hisobga oladi.
 */
public interface ButtonLabelResolver {

    Optional<BotCommand> resolve(String label);
}
