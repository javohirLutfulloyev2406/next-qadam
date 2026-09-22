package uz.nextqadam.bot.admin.impl;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import uz.nextqadam.bot.admin.ActivityLogService;
import uz.nextqadam.bot.admin.UserActivityLog;
import uz.nextqadam.bot.admin.UserActivityLog.ActionType;
import uz.nextqadam.bot.admin.UserActivityLogRepository;
import uz.nextqadam.bot.user.UserRepository;

@Service
public class ActivityLogServiceImpl implements ActivityLogService {

    private static final Logger log = LoggerFactory.getLogger(ActivityLogServiceImpl.class);

    private final UserActivityLogRepository userActivityLogRepository;
    private final UserRepository userRepository;

    public ActivityLogServiceImpl(UserActivityLogRepository userActivityLogRepository, UserRepository userRepository) {
        this.userActivityLogRepository = userActivityLogRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Async
    public void logActivity(UUID userId, ActionType type, String detail) {
        try {
            UserActivityLog entry = UserActivityLog.builder()
                    .user(userRepository.getReferenceById(userId))
                    .actionType(type)
                    .actionDetail(detail)
                    .build();
            userActivityLogRepository.save(entry);
        } catch (Exception e) {
            log.warn("Faollik logini saqlashda xatolik. userId={}, type={}", userId, type, e);
        }
    }
}
