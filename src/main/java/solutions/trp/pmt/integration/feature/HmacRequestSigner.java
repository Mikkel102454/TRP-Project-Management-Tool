package solutions.trp.pmt.integration.feature;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.function.Supplier;

public class HmacRequestSigner {
    private final String authId;
    private final String authKey;
    private final Clock clock;
    private final Supplier<byte[]> nonceSupplier;

    public HmacRequestSigner(String authId, String authKey, Clock clock, Supplier<byte[]> nonceSupplier) {
        this.authId = authId;
        this.authKey = authKey;
        this.clock = clock;
        this.nonceSupplier = nonceSupplier;
    }

    public String authorization(String method, String endpoint, String body) {
        String timestamp = DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(
                OffsetDateTime.ofInstant(clock.instant().truncatedTo(ChronoUnit.SECONDS), ZoneOffset.UTC));
        String nonce = HexFormat.of().formatHex(nonceSupplier.get());
        String canonical = canonical(timestamp, nonce, method, endpoint, body);
        return "HMAC id=" + authId + ", ts=" + timestamp + ", nonce=" + nonce + ", mac=" + digest(canonical);
    }

    public static String canonical(String timestamp, String nonce, String method, String endpoint, String body) {
        return String.join("\n", timestamp, nonce, method, endpoint, body);
    }

    private String digest(String message) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(authKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(message.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HMAC-SHA256 is not available", e);
        }
    }
}
