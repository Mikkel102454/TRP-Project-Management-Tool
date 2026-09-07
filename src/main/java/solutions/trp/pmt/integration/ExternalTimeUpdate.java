package solutions.trp.pmt.integration;

import java.time.Instant;

/** The timestamps confirmed by a provider after updating a completed time registration. */
public record ExternalTimeUpdate(
        Instant previousStart,
        Instant previousEnd,
        Instant start,
        Instant end
) {}
