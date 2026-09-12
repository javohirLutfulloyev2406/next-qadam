package uz.nextqadam.bot.track.impl;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import uz.nextqadam.bot.track.CheckIn;
import uz.nextqadam.bot.track.CheckInRepository;
import uz.nextqadam.bot.track.JournalEntry;
import uz.nextqadam.bot.track.JournalEntryRepository;
import uz.nextqadam.bot.track.TrackService;

@Service
public class TrackServiceImpl implements TrackService {

    private final CheckInRepository checkInRepository;
    private final JournalEntryRepository journalEntryRepository;

    public TrackServiceImpl(CheckInRepository checkInRepository, JournalEntryRepository journalEntryRepository) {
        this.checkInRepository = checkInRepository;
        this.journalEntryRepository = journalEntryRepository;
    }

    @Override
    public CheckIn recordCheckIn(UUID userId, CheckIn.Type type, String rawText) {
        // TODO: implementatsiya
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public JournalEntry addJournalEntry(UUID userId, String content, JournalEntry.SourceType sourceType) {
        // TODO: implementatsiya
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public List<CheckIn> getRecentCheckIns(UUID userId, int days) {
        // TODO: implementatsiya
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public List<JournalEntry> getJournalEntries(UUID userId) {
        // TODO: implementatsiya
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
