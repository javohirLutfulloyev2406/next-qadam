package uz.nextqadam.bot.goal;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface GoalRepository extends JpaRepository<Goal, UUID> {

    List<Goal> findAllByUserIdAndStatus(UUID userId, Goal.Status status);
}
