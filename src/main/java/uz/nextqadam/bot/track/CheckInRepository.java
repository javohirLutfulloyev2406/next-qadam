package uz.nextqadam.bot.track;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CheckInRepository extends JpaRepository<CheckIn, UUID> {

    Optional<CheckIn> findByUserIdAndDateAndType(UUID userId, LocalDate date, CheckIn.Type type);

    List<CheckIn> findAllByUserIdAndDateBetween(UUID userId, LocalDate from, LocalDate to);

    /**
     * Admin statistikasidagi "bugun faol" hisobiga qo'shiladigan — bugun kamida bitta CheckIn
     * yozgan foydalanuvchilarning distinct ID ro'yxati.
     */
    @Query("SELECT DISTINCT c.user.id FROM CheckIn c WHERE c.date = :date")
    List<UUID> findDistinctUserIdsByDate(LocalDate date);
}
