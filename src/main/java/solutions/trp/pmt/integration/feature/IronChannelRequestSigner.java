package solutions.trp.pmt.integration.feature;

import com.google.gson.JsonObject;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Clock;
import java.util.Base64;
import java.util.HexFormat;

/** Generates request-bound JWTs compatible with IronChannel's JWTVerify. */
public class IronChannelRequestSigner {
    private static final Base64.Encoder BASE64_URL = Base64.getUrlEncoder().withoutPadding();
    private static final String HEADER = BASE64_URL.encodeToString(
            "{\"typ\":\"JWT\",\"alg\":\"HS256\"}".getBytes(StandardCharsets.UTF_8));
    private final String appId;
    private final String secret;
    private final Clock clock;

    public IronChannelRequestSigner(String appId, String secret, Clock clock) {
        this.appId = appId;
        this.secret = secret;
        this.clock = clock;
    }

    public String authorization(String endpoint, byte[] body) {
        try {
            long issuedAt = clock.instant().getEpochSecond();
            JsonObject payload = new JsonObject();
            payload.addProperty("app_id", appId);
            payload.addProperty("endpoint", endpoint);
            payload.addProperty("iat", issuedAt);
            payload.addProperty("exp", issuedAt + 30);
            payload.addProperty("body_hash", "sha256:" + HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(body)));

            String signingInput = HEADER + "." + BASE64_URL.encodeToString(
                    payload.toString().getBytes(StandardCharsets.UTF_8));
            Mac mac = Mac.getInstance("HmacSHA256");
            // IronChannel uses the literal secret, including when it looks Base64URL-encoded.
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return "Bearer " + signingInput + "." + BASE64_URL.encodeToString(
                    mac.doFinal(signingInput.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("IronChannel JWT signing is unavailable", e);
        }
    }
}
