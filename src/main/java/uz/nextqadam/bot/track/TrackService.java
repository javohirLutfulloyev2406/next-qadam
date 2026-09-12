package uz.nextqadam.bot.track;

import java.util.List;
import java.util.UUID;

public interface TrackService {

    CheckIn recordCheckIn(UUID userId, CheckIn.Type type, String rawText);

    JournalEntry addJournalEntry(UUID userId, String content, JournalEntry.SourceType sourceType);

    List<CheckIn> getRecentCheckIns(UUID userId, int days);

    List<JournalEntry> getJournalEntries(UUID userId);
}
