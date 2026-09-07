package solutions.trp.pmt.integration;

import java.time.LocalDateTime;
import java.util.Map;

public record ExternalWorkItem(
        String id,
        String title,
        String status,
        String release,
        String module,
        String type,
        String accountId,
        LocalDateTime createdAt,
        String description,
        String developmentNotes,
        Map<String, String> metadata
) {
}
