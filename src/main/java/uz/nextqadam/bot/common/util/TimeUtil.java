package uz.nextqadam.bot.common.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Loyihaning barcha "bugun" hisob-kitoblari va foydalanuvchiga ko'rsatiladigan sanalar shu klass
 * orqali Asia/Tashkent vaqt zonasiga moslashtiriladi — server operatsion tizim darajasida UTC'da
 * ishlaganida ham (masalan LocalDate.now() zone ko'rsatilmasdan chaqirilsa) natija noto'g'ri
 * bo'lib qolmasligi uchun.
 */
public final class TimeUtil {

    public static final ZoneId TASHKENT_ZONE = ZoneId.of("Asia/Tashkent");

    private static final DateTimeFormatter DISPLAY_FORMATTER =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").withZone(TASHKENT_ZONE);
    private static final DateTimeFormatter DATE_ONLY_FORMATTER =
            DateTimeFormatter.ofPattern("dd.MM.yyyy").withZone(TASHKENT_ZONE);

    private TimeUtil() {
    }

    public static LocalDate todayInTashkent() {
        return LocalDate.now(TASHKENT_ZONE);
    }

    public static String formatForDisplay(Instant instant) {
        return DISPLAY_FORMATTER.format(instant);
    }

    public static String formatDateOnly(Instant instant) {
        return DATE_ONLY_FORMATTER.format(instant);
    }
}
