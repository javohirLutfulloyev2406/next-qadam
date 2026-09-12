package uz.nextqadam.bot.goal;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskRepository extends JpaRepository<Task, UUID> {

    List<Task> findAllByGoalId(UUID goalId);

    List<Task> findAllByStatusAndDueDateBefore(Task.Status status, Instant dueDate);
}
