package solutions.trp.pmt.integration.feature;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class IronChannelRequestSignerTest {
    private static final Instant NOW = Instant.ofEpochSecond(1786443330);

    @Test
    void matchesAnIndependentlyGeneratedJwtVector() throws Exception {
        byte[] body = "{\"release\":\"Æøå 日本 🚀\"}".getBytes(StandardCharsets.UTF_8);
        String header = new IronChannelRequestSigner("abc123", "c2VjcmV0LWtleQ",
                Clock.fixed(NOW, ZoneOffset.UTC)).authorization("feature/list/?page=2", body);
        // Python stdlib vector; the Base64-looking secret must be used literally.
        assertEquals("Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJhcHBfaWQiOiJhYmMxMjMiLCJlbmRwb2ludCI6ImZlYXR1cmUvbGlzdC8_cGFnZT0yIiwiaWF0IjoxNzg2NDQzMzMwLCJleHAiOjE3ODY0NDMzNjAsImJvZHlfaGFzaCI6InNoYTI1Njo3ODg3ODk4YjYyMGU0NzYzZGNmYWE1MWEyMmQ0Mjc5OTE2M2U0NTZlYWMwNTc3MzVhNWVmMzY5MzE2NjBiZDU3In0.-c_OoTpfCSmFZV50lMPVN-IrNJ9pw2tKP-UF7Fh7KTM", header);
        verify(header, "c2VjcmV0LWtleQ", "abc123", "feature/list/?page=2", body, NOW.getEpochSecond());
    }

    @Test
    void hashesEmptyBodiesAndGeneratesFreshClaimsForEachRequest() throws Exception {
        Clock clock = mock(Clock.class);
        when(clock.instant()).thenReturn(NOW.plusNanos(999999999), NOW.plusSeconds(10));
        IronChannelRequestSigner signer = new IronChannelRequestSigner("abc123", "secret-key", clock);
        String first = signer.authorization("account/activity/", new byte[0]);
        String second = signer.authorization("account/activity/", new byte[0]);
        verify(first, "secret-key", "abc123", "account/activity/", new byte[0], NOW.getEpochSecond());
        verify(second, "secret-key", "abc123", "account/activity/", new byte[0], NOW.getEpochSecond() + 10);
        assertNotEquals(first, second);
        assertEquals("sha256:e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                decode(first.substring(7).split("\\.")[1]).get("body_hash").getAsString());
    }

    static void verify(String authorization, String secret, String appId, String endpoint,
                       byte[] body, long issuedAt) throws Exception {
        assertNotNull(authorization);
        assertTrue(authorization.startsWith("Bearer "));
        String[] parts = authorization.substring(7).split("\\.");
        assertEquals(3, parts.length);
        for (String part : parts) assertTrue(part.matches("[A-Za-z0-9_-]+"));
        JsonObject header = decode(parts[0]);
        assertEquals("JWT", header.get("typ").getAsString());
        assertEquals("HS256", header.get("alg").getAsString());
        JsonObject payload = decode(parts[1]);
        assertEquals(appId, payload.get("app_id").getAsString());
        assertEquals(endpoint, payload.get("endpoint").getAsString());
        assertEquals(issuedAt, payload.get("iat").getAsLong());
        assertEquals(issuedAt + 30, payload.get("exp").getAsLong());
        assertEquals("sha256:" + HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(body)),
                payload.get("body_hash").getAsString());
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        assertArrayEquals(mac.doFinal((parts[0] + "." + parts[1]).getBytes(StandardCharsets.UTF_8)),
                Base64.getUrlDecoder().decode(parts[2]));
    }

    private static JsonObject decode(String part) {
        return JsonParser.parseString(new String(Base64.getUrlDecoder().decode(part), StandardCharsets.UTF_8)).getAsJsonObject();
    }
}
