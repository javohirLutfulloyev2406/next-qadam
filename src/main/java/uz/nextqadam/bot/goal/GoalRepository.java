package uz.nextqadam.bot.goal;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface GoalRepository extends JpaRepository<Goal, UUID> {

    List<Goal> findAllByUserIdAndStatus(UUID userId, Goal.Status status);

    Optional<Goal> findByUserIdAndTitle(UUID userId, String title);

    /**
     * Foydalanuvchining eng so'nggi yaratilgan faol Goal'i — Goal Drift Detection uchun.
     */
    Optional<Goal> findFirstByUserIdAndStatusOrderByCreatedAtDesc(UUID userId, Goal.Status status);
}
