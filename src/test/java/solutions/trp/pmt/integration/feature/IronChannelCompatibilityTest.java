package solutions.trp.pmt.integration.feature;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;

import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/** Optional cross-language contract test against an actual Feature API checkout. */
@EnabledIfEnvironmentVariable(named = "FEATURE_API_SOURCE", matches = ".+")
class IronChannelCompatibilityTest {
    @TempDir Path temporaryDirectory;

    @Test
    void phpAcceptsJavaTokensAndRejectsTampering() throws Exception {
        Path source = Path.of(System.getenv("FEATURE_API_SOURCE"));
        assertTrue(Files.isRegularFile(source.resolve("Core/Authorization.php")));
        int port;
        try (ServerSocket socket = new ServerSocket(0)) { port = socket.getLocalPort(); }
        Path router = temporaryDirectory.resolve("router.php");
        try (var stream = getClass().getResourceAsStream("/ironchannel/router.php")) {
            assertNotNull(stream);
            Files.copy(stream, router);
        }
        Process php = new ProcessBuilder("php", "-S", "127.0.0.1:" + port, router.toString())
                .redirectErrorStream(true).redirectOutput(temporaryDirectory.resolve("php.log").toFile()).start();
        try (HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(1)).build()) {
            URI root = URI.create("http://127.0.0.1:" + port + "/feature-api/");
            boolean ready = false;
            for (int attempt = 0; attempt < 100 && php.isAlive(); attempt++) {
                try {
                    send(http, root, "Bearer invalid", new byte[0]);
                    ready = true;
                    break;
                } catch (java.io.IOException e) { Thread.sleep(50); }
            }
            assertTrue(ready, () -> "PHP verifier did not start: " + php.info());
            Clock clock = Clock.systemUTC();
            IronChannelRequestSigner signer = new IronChannelRequestSigner("abc123", "secret-key", clock);
            byte[] empty = new byte[0];
            String getEndpoint = "account/7/profile/";
            assertEquals(200, send(http, root.resolve(getEndpoint), signer.authorization(getEndpoint, empty), empty));
            byte[] body = "{\"release\":\"Æøå 日本 🚀\"}".getBytes(StandardCharsets.UTF_8);
            String endpoint = "feature/list/?page=2";
            String token = signer.authorization(endpoint, body);
            assertEquals(200, send(http, root.resolve(endpoint), token, body));
            assertEquals(401, send(http, root.resolve(endpoint), token, "{}".getBytes(StandardCharsets.UTF_8)));
            assertEquals(401, send(http, root.resolve("feature/list/"), token, body));
            assertEquals(401, send(http, root.resolve(endpoint),
                    new IronChannelRequestSigner("abc123", "wrong-secret", clock).authorization(endpoint, body), body));
            assertEquals(401, send(http, root.resolve(endpoint),
                    new IronChannelRequestSigner("unknown-app", "secret-key", clock).authorization(endpoint, body), body));
            Clock expired = Clock.fixed(Instant.now().minusSeconds(60), ZoneOffset.UTC);
            assertEquals(401, send(http, root.resolve(endpoint),
                    new IronChannelRequestSigner("abc123", "secret-key", expired).authorization(endpoint, body), body));
        } finally {
            php.destroy();
            if (!php.waitFor(5, TimeUnit.SECONDS)) php.destroyForcibly();
        }
    }

    private static int send(HttpClient client, URI uri, String authorization, byte[] body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(2))
                .header("Authorization", authorization).header("Content-Type", "application/json")
                .method(body.length == 0 ? "GET" : "POST", HttpRequest.BodyPublishers.ofByteArray(body)).build();
        return client.send(request, HttpResponse.BodyHandlers.ofString()).statusCode();
    }
}
