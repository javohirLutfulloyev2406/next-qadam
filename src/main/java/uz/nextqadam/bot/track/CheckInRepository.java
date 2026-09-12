package uz.nextqadam.bot.track;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CheckInRepository extends JpaRepository<CheckIn, UUID> {

    Optional<CheckIn> findByUserIdAndDateAndType(UUID userId, LocalDate date, CheckIn.Type type);

    List<CheckIn> findAllByUserIdAndDateBetween(UUID userId, LocalDate from, LocalDate to);
}
