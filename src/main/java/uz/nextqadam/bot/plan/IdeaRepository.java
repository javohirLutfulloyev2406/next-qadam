package uz.nextqadam.bot.plan;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface IdeaRepository extends JpaRepository<Idea, UUID> {

    List<Idea> findByUser_IdOrderByCreatedAtDesc(UUID userId);
}
