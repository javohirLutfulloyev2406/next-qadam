package uz.nextqadam.bot.nudge;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ReminderRepository extends JpaRepository<Reminder, UUID> {

    List<Reminder> findAllByStatusAndScheduledAtBefore(Reminder.Status status, Instant scheduledAt);
}
