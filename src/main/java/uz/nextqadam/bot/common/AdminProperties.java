package uz.nextqadam.bot.common;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "telegram.admin")
public class AdminProperties {

    private String ids = "";

    public String getIds() {
        return ids;
    }

    public void setIds(String ids) {
        this.ids = ids;
    }

    /**
     * "telegram.admin.ids" — vergul bilan ajratilgan Telegram ID'lar ro'yxati (.env'dagi
     * ADMIN_TELEGRAM_IDS). Bo'sh bo'lsa hech kim admin emas — bu ataylab xavfsiz standart holat.
     */
    public Set<Long> getAdminTelegramIds() {
        if (ids == null || ids.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(ids.split(","))
                .map(String::trim)
                .filter(part -> !part.isEmpty())
                .map(Long::parseLong)
                .collect(Collectors.toSet());
    }
}
