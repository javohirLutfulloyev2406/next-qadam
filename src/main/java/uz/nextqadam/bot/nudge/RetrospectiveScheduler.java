package uz.nextqadam.bot.nudge;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import uz.nextqadam.bot.ai.dto.GoalDriftResult;
import uz.nextqadam.bot.ai.dto.WeeklyRetrospective;
import uz.nextqadam.bot.common.HtmlEscaper;
import uz.nextqadam.bot.common.LocalizationService;
import uz.nextqadam.bot.common.telegram.TelegramExecutor;
import uz.nextqadam.bot.track.TrackHandler;
import uz.nextqadam.bot.track.TrackService;
import uz.nextqadam.bot.user.User;
import uz.nextqadam.bot.user.UserRepository;

@Component
public class RetrospectiveScheduler {

    private static final Logger log = LoggerFactory.getLogger(RetrospectiveScheduler.class);

    private final UserRepository userRepository;
    private final TrackService trackService;
    private final TrackHandler trackHandler;
    private final TelegramExecutor telegramExecutor;
    private final LocalizationService localizationService;

    public RetrospectiveScheduler(UserRepository userRepository, TrackService trackService,
                                   TrackHandler trackHandler, TelegramExecutor telegramExecutor,
                                   LocalizationService localizationService) {
        this.userRepository = userRepository;
        this.trackService = trackService;
        this.trackHandler = trackHandler;
        this.telegramExecutor = telegramExecutor;
        this.localizationService = localizationService;
    }

    // MVP uchun barcha foydalanuvchilarga yuboriladi. Kelajakda faqat kamida bitta faol Goal'i bor
    // foydalanuvchilarga cheklash mumkin (masalan GoalRepository orqali oldindan filtrlab).
    @Scheduled(cron = "0 0 20 * * SUN", zone = "Asia/Tashkent")
    public void runWeeklyRetrospective() {
        List<User> users = userRepository.findAll();
        for (User user : users) {
            try {
                sendRetrospectiveAndDriftCheck(user);
            } catch (Exception e) {
                log.error("Haftalik retrospektiva/drift tekshiruvida xatolik. userId={}", user.getId(), e);
            }
        }
    }

    private void sendRetrospectiveAndDriftCheck(User user) {
        WeeklyRetrospective retrospective = trackService.generateWeeklyRetrospective(user.getId());
        String text = localizationService.get(user.getLanguage(), "track.retro.title") + "\n\n"
                + HtmlEscaper.escape(retrospective.narrative()) + "\n\n"
                + localizationService.get(user.getLanguage(), "track.retro.next_week",
                        HtmlEscaper.escape(retrospective.recommendation()));
        telegramExecutor.sendMessage(user.getTelegramId(), text);

        GoalDriftResult drift = trackService.checkGoalDrift(user.getId());
        trackHandler.sendDriftWarningIfBelowThreshold(user.getTelegramId(), drift, user.getLanguage());
    }
}
