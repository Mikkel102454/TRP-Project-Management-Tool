package solutions.trp.pmt.dto;

import java.util.List;

public class TimeValidationDto {
    public List<TimeDto> entries;
    public List<String> missingDays;
    public List<Integer> invalidEntryIds;

    public TimeValidationDto(
            List<TimeDto> entries,
            List<String> missingDays,
            List<Integer> invalidEntryIds
    ) {
        this.entries = entries;
        this.missingDays = missingDays;
        this.invalidEntryIds = invalidEntryIds;
    }
}