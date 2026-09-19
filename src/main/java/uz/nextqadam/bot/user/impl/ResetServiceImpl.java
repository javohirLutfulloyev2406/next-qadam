package uz.nextqadam.bot.user.impl;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import uz.nextqadam.bot.common.enums.ToneType;
import uz.nextqadam.bot.common.exception.NextQadamException;
import uz.nextqadam.bot.goal.GoalRepository;
import uz.nextqadam.bot.goal.MilestoneRepository;
import uz.nextqadam.bot.goal.TaskRepository;
import uz.nextqadam.bot.memory.MemoryItemRepository;
import uz.nextqadam.bot.nudge.ReminderRepository;
import uz.nextqadam.bot.plan.IdeaRepository;
import uz.nextqadam.bot.track.CheckInRepository;
import uz.nextqadam.bot.track.JournalEntryRepository;
import uz.nextqadam.bot.user.ResetService;
import uz.nextqadam.bot.user.User;
import uz.nextqadam.bot.user.UserRepository;

@Service
public class ResetServiceImpl implements ResetService {

    private final UserRepository userRepository;
    private final GoalRepository goalRepository;
    private final MilestoneRepository milestoneRepository;
    private final TaskRepository taskRepository;
    private final CheckInRepository checkInRepository;
    private final JournalEntryRepository journalEntryRepository;
    private final ReminderRepository reminderRepository;
    private final MemoryItemRepository memoryItemRepository;
    private final IdeaRepository ideaRepository;

    public ResetServiceImpl(UserRepository userRepository, GoalRepository goalRepository,
                             MilestoneRepository milestoneRepository, TaskRepository taskRepository,
                             CheckInRepository checkInRepository, JournalEntryRepository journalEntryRepository,
                             ReminderRepository reminderRepository, MemoryItemRepository memoryItemRepository,
                             IdeaRepository ideaRepository) {
        this.userRepository = userRepository;
        this.goalRepository = goalRepository;
        this.milestoneRepository = milestoneRepository;
        this.taskRepository = taskRepository;
        this.checkInRepository = checkInRepository;
        this.journalEntryRepository = journalEntryRepository;
        this.reminderRepository = reminderRepository;
        this.memoryItemRepository = memoryItemRepository;
        this.ideaRepository = ideaRepository;
    }

    @Override
    @Transactional
    public void softReset(UUID userId) {
        reminderRepository.softDeleteAllByUserId(userId);
        taskRepository.softDeleteAllByUserId(userId);
        milestoneRepository.softDeleteAllByUserId(userId);
        goalRepository.softDeleteAllByUserId(userId);
        checkInRepository.softDeleteAllByUserId(userId);
        journalEntryRepository.softDeleteAllByUserId(userId);
        memoryItemRepository.softDeleteAllByUserId(userId);
        ideaRepository.softDeleteAllByUserId(userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NextQadamException("Foydalanuvchi topilmadi: " + userId));
        user.setName(null);
        user.setTonePreference(ToneType.NORMAL);
        user.setTimezone(null);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void hardDelete(UUID userId) {
        // FK xatosiga yo'l qo'ymaslik uchun avval child jadvallar, eng oxirida User'ning o'zi.
        reminderRepository.hardDeleteAllByUserId(userId);
        taskRepository.hardDeleteAllByUserId(userId);
        milestoneRepository.hardDeleteAllByUserId(userId);
        goalRepository.hardDeleteAllByUserId(userId);
        checkInRepository.hardDeleteAllByUserId(userId);
        journalEntryRepository.hardDeleteAllByUserId(userId);
        memoryItemRepository.hardDeleteAllByUserId(userId);
        ideaRepository.hardDeleteAllByUserId(userId);
        userRepository.deleteById(userId);
    }
}
