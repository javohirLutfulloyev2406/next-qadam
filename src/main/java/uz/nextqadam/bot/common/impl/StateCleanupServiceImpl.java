package uz.nextqadam.bot.common.impl;

import org.springframework.stereotype.Service;

import uz.nextqadam.bot.common.StateCleanupService;
import uz.nextqadam.bot.goal.GoalHandler;
import uz.nextqadam.bot.plan.PlanHandler;
import uz.nextqadam.bot.track.TrackHandler;
import uz.nextqadam.bot.user.OnboardingHandler;
import uz.nextqadam.bot.user.ProfileHandler;

@Service
public class StateCleanupServiceImpl implements StateCleanupService {

    private final OnboardingHandler onboardingHandler;
    private final GoalHandler goalHandler;
    private final PlanHandler planHandler;
    private final ProfileHandler profileHandler;
    private final TrackHandler trackHandler;

    public StateCleanupServiceImpl(OnboardingHandler onboardingHandler, GoalHandler goalHandler,
                                    PlanHandler planHandler, ProfileHandler profileHandler, TrackHandler trackHandler) {
        this.onboardingHandler = onboardingHandler;
        this.goalHandler = goalHandler;
        this.planHandler = planHandler;
        this.profileHandler = profileHandler;
        this.trackHandler = trackHandler;
    }

    @Override
    public void clearAll(Long chatId) {
        onboardingHandler.clearState(chatId);
        goalHandler.clearState(chatId);
        planHandler.clearState(chatId);
        profileHandler.clearState(chatId);
        trackHandler.clearState(chatId);
    }
}
