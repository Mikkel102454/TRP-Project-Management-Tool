package solutions.trp.pmt.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JsonPaperTest {
    @Test
    void workDaysPutLastWeekBeforeThisWeek() {
        List<LocalDate> workDays = JsonPaper.workDaysFor(
                LocalDate.of(2026, 7, 30)
        );

        assertEquals(
                List.of(
                        LocalDate.of(2026, 7, 20),
                        LocalDate.of(2026, 7, 21),
                        LocalDate.of(2026, 7, 22),
                        LocalDate.of(2026, 7, 23),
                        LocalDate.of(2026, 7, 24),
                        LocalDate.of(2026, 7, 27),
                        LocalDate.of(2026, 7, 28),
                        LocalDate.of(2026, 7, 29),
                        LocalDate.of(2026, 7, 30),
                        LocalDate.of(2026, 7, 31)
                ),
                workDays
        );
    }
}
