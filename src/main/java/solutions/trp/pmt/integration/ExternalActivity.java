package solutions.trp.pmt.integration;

import java.time.LocalDateTime;

public record ExternalActivity(String registrationId, String workItemId, String accountId, LocalDateTime startedAt) {
}
