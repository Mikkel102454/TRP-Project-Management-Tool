package solutions.trp.pmt.integration.feature;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import solutions.trp.pmt.integration.ExternalWorkItem;
import solutions.trp.pmt.integration.ExternalActivity;
import solutions.trp.pmt.integration.ExternalTimeUpdate;
import solutions.trp.pmt.integration.IntegrationException;
import solutions.trp.pmt.integration.IntegrationFailure;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FeatureApiClientTest {
    private HttpServer server;
    private final java.util.concurrent.atomic.AtomicReference<Throwable> serverFailure = new java.util.concurrent.atomic.AtomicReference<>();

    @AfterEach
    void stopServer() {
        if (server != null) server.stop(0);
        if (serverFailure.get() != null) throw new AssertionError("HTTP server assertion failed", serverFailure.get());
    }

    @Test
    void springSelectsTheConfigurationConstructor() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean(FeatureApiProperties.class);
            context.registerBean(FeatureApiClient.class);
            context.refresh();
            assertNotNull(context.getBean(FeatureApiClient.class));
        }
    }

    @Test
    void listsEveryFeatureAndPreservesRawStatus() throws Exception {
        server = server(exchange -> {
            assertEquals("POST", exchange.getRequestMethod());
            assertTrue(exchange.getRequestHeaders().getFirst("Authorization").startsWith("Bearer "));
            assertEquals("{\"release\":\"MixedCase\"}", new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            respond(exchange, 200, """
                    {"12":{"status":"discus","type":"new","release":"MixedCase","header":"One","module":"core","time_create":"2026-08-01 12:00:00","account_id":"7"},
                     "13":{"status":"closed-custom","type":"bug","release":"MixedCase","header":"Two","module":"web","time_create":"2026-08-02 13:00:00","account_id":"8"}}
                    """);
        });

        List<ExternalWorkItem> items = client(Duration.ofSeconds(2)).listWorkItems("MixedCase");

        assertEquals(2, items.size());
        assertEquals("discus", items.getFirst().status());
        assertEquals("7", items.getFirst().accountId());
        assertEquals("closed-custom", items.get(1).status());
    }

    @Test
    void acceptsAnEmptyReleaseResult() throws Exception {
        server = server(exchange -> respond(exchange, 200, "[]"));
        assertTrue(client(Duration.ofSeconds(2)).listWorkItems("DoesNotExist").isEmpty());
    }

    @Test
    void acceptsAnEmptyAccountActivityArrayBeforeStartingTime() throws Exception {
        server = server(exchange -> respond(exchange, 200, "[]"));

        assertTrue(client(Duration.ofSeconds(2)).getActivities("7").isEmpty());
    }

    @Test
    void stillRejectsAnEmptyArrayForObjectOperations() throws Exception {
        server = server(exchange -> respond(exchange, 200, "[]"));

        IntegrationException failure = assertThrows(IntegrationException.class,
                () -> client(Duration.ofSeconds(2)).startTime("7", "12"));

        assertEquals("PM returned an invalid response", failure.getMessage());
    }

    @Test
    void mapsAuthenticationErrorsWithoutLeakingRemoteDetails() throws Exception {
        server = server(exchange -> respond(exchange, 401, "{\"error\":\"Invalid JWT signature secret material\"}"));

        IntegrationException failure = assertThrows(IntegrationException.class,
                () -> client(Duration.ofSeconds(2)).listWorkItems("TRP"));

        assertEquals(IntegrationFailure.AUTHENTICATION, failure.getFailure());
        assertFalse(failure.getMessage().contains("secret material"));
    }

    @Test
    @ExtendWith(OutputCaptureExtension.class)
    void logsProfileHttpFailuresWithoutRemoteResponseDetails(CapturedOutput output) throws Exception {
        server = server(exchange -> respond(exchange, 502, "private upstream response"));

        IntegrationException failure = assertThrows(IntegrationException.class,
                () -> client(Duration.ofSeconds(2)).validateUser("7"));

        assertEquals(IntegrationFailure.UNAVAILABLE, failure.getFailure());
        assertEquals("PM service is unavailable", failure.getMessage());
        assertTrue(output.getOut().contains("method=GET, endpoint=account/7/profile/, status=502"));
        assertFalse(output.getAll().contains("private upstream response"));
        assertFalse(output.getAll().contains("secret-key"));
    }

    @Test
    @ExtendWith(OutputCaptureExtension.class)
    void logsProfileParseFailuresWithoutRemoteResponseDetails(CapturedOutput output) throws Exception {
        server = server(exchange -> respond(exchange, 200, "{\"private upstream response\":"));

        IntegrationException failure = assertThrows(IntegrationException.class,
                () -> client(Duration.ofSeconds(2)).validateUser("7"));

        assertEquals(IntegrationFailure.UNAVAILABLE, failure.getFailure());
        assertTrue(output.getOut().contains("method=GET, endpoint=account/7/profile/, exception=JsonSyntaxException"));
        assertFalse(output.getAll().contains("private upstream response"));
    }

    @Test
    void mapsTimeouts() throws Exception {
        server = server(exchange -> {
            try { Thread.sleep(250); } catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); }
            respond(exchange, 200, "{}");
        });

        IntegrationException failure = assertThrows(IntegrationException.class,
                () -> client(Duration.ofMillis(40)).listWorkItems("TRP"));
        assertEquals(IntegrationFailure.TIMEOUT, failure.getFailure());
    }

    @Test
    void rejectsMissingAccountProfiles() throws Exception {
        server = server(exchange -> respond(exchange, 200, ""));
        IntegrationException failure = assertThrows(IntegrationException.class,
                () -> client(Duration.ofSeconds(2)).validateUser("7"));
        assertEquals(IntegrationFailure.INVALID_ACCOUNT, failure.getFailure());
    }

    @Test
    void readsGlobalActivityWithoutRequiringTheApiToReturnAccountIds() throws Exception {
        server = server(exchange -> respond(exchange, 200, """
                {"99":{"start":"2026-08-11 10:00:00","feature":"12"}}
                """));

        List<ExternalActivity> activities = client(Duration.ofSeconds(2)).getActivities();

        assertEquals(1, activities.size());
        assertEquals("99", activities.getFirst().registrationId());
        assertEquals("12", activities.getFirst().workItemId());
        assertNull(activities.getFirst().accountId());
    }

    @Test
    void suppliesTheRequestedAccountIdForAccountSpecificActivity() throws Exception {
        server = server(exchange -> {
            assertEquals("/account/7/activity/", exchange.getRequestURI().getPath());
            respond(exchange, 200, """
                    {"99":{"start":"2026-08-11 10:00:00","feature":"12"}}
                    """);
        });

        ExternalActivity activity = client(Duration.ofSeconds(2)).getActivities("7").getFirst();

        assertEquals("7", activity.accountId());
    }

    @Test
    void resolvesActiveAccountIdsFromTheExistingFeatureTimeEndpoint() throws Exception {
        server = server(exchange -> {
            assertEquals("/feature/12/time/", exchange.getRequestURI().getPath());
            respond(exchange, 200, """
                    {"99":{"start":"2026-08-11 10:00:00","stop":null,"account_id":"7"},
                     "98":{"start":"2026-08-11 09:00:00","stop":"2026-08-11 09:30:00","account_id":"8"}}
                    """);
        });

        List<ExternalActivity> activities = client(Duration.ofSeconds(2)).getWorkItemActivities("12");

        assertEquals(1, activities.size());
        assertEquals("99", activities.getFirst().registrationId());
        assertEquals("12", activities.getFirst().workItemId());
        assertEquals("7", activities.getFirst().accountId());
    }

    @Test
    void confirmsThatAStoppedRegistrationWasRetainedByPm() throws Exception {
        server = server(exchange -> {
            assertEquals("GET", exchange.getRequestMethod());
            assertEquals("/feature/12/time/", exchange.getRequestURI().getPath());
            respond(exchange, 200, """
                    {"99":{"start":"2026-08-11 10:00:00","stop":"2026-08-11 10:30:00","account_id":"7"}}
                    """);
        });

        assertTrue(client(Duration.ofSeconds(2)).timeEntryExists("7", "12", "99"));
    }

    @Test
    void reportsThatPmDeletedAStoppedRegistration() throws Exception {
        server = server(exchange -> respond(exchange, 200, "[]"));

        assertFalse(client(Duration.ofSeconds(2)).timeEntryExists("7", "12", "99"));
    }

    @Test
    void doesNotMatchARegistrationOwnedByAnotherAccount() throws Exception {
        server = server(exchange -> respond(exchange, 200, """
                {"99":{"start":"2026-08-11 10:00:00","stop":"2026-08-11 10:30:00","account_id":"8"}}
                """));

        assertFalse(client(Duration.ofSeconds(2)).timeEntryExists("7", "12", "99"));
    }

    @Test
    void updatesACompletedTimeRegistrationAndReturnsConfirmedTimestamps() throws Exception {
        server = server(exchange -> {
            assertEquals("POST", exchange.getRequestMethod());
            assertEquals("/account/7/time/", exchange.getRequestURI().getPath());
            assertTrue(exchange.getRequestHeaders().getFirst("Authorization").startsWith("Bearer "));
            assertEquals("{\"feature\":\"12\",\"time\":\"99\",\"start\":\"2026-08-11 09:15:00\",\"stop\":\"2026-08-11 10:45:00\"}",
                    new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            respond(exchange, 200, """
                    {"result":"success",
                     "start_previous":"2026-08-11 09:00:00","stop_previous":"2026-08-11 10:00:00",
                     "start":"2026-08-11 09:15:00","stop":"2026-08-11 10:45:00"}
                    """);
        });

        ZoneId zone = ZoneId.systemDefault();
        Instant requestedStart = LocalDateTime.parse("2026-08-11T09:15:00").atZone(zone).toInstant();
        Instant requestedEnd = LocalDateTime.parse("2026-08-11T10:45:00").atZone(zone).toInstant();

        ExternalTimeUpdate update = client(Duration.ofSeconds(2))
                .updateTime("7", "12", "99", requestedStart, requestedEnd);

        assertEquals(LocalDateTime.parse("2026-08-11T09:00:00").atZone(zone).toInstant(), update.previousStart());
        assertEquals(LocalDateTime.parse("2026-08-11T10:00:00").atZone(zone).toInstant(), update.previousEnd());
        assertEquals(requestedStart, update.start());
        assertEquals(requestedEnd, update.end());
    }

    @Test
    void rejectsAnIncompleteTimeUpdateConfirmation() throws Exception {
        server = server(exchange -> respond(exchange, 200, "{\"result\":\"success\",\"start\":\"2026-08-11 09:15:00\"}"));
        Instant start = Instant.parse("2026-08-11T09:15:00Z");

        IntegrationException failure = assertThrows(IntegrationException.class,
                () -> client(Duration.ofSeconds(2)).updateTime("7", "12", "99", start, start.plusSeconds(60)));

        assertEquals(IntegrationFailure.UNAVAILABLE, failure.getFailure());
        assertEquals("PM returned an invalid time update response", failure.getMessage());
    }

    @Test
    void sendsUnicodeJsonWithoutChangingTheHashedBytes() throws Exception {
        server = server(exchange -> {
            assertEquals("{\"release\":\"Æøå 日本 🚀\"}",
                    new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            respond(exchange, 200, "{}");
        });
        assertTrue(client(Duration.ofSeconds(2)).listWorkItems("Æøå 日本 🚀").isEmpty());
    }

    @Test
    void readsProfilesAndFeaturesAndStartsAndStopsTimers() throws Exception {
        server = server(exchange -> {
            String path = exchange.getRequestURI().getPath();
            String result = switch (path) {
                case "/account/7/profile/" -> "{\"name\":\"Test user\"}";
                case "/feature/12/info/" -> "{\"id\":\"12\",\"header\":\"Test feature\"}";
                case "/account/7/start/" -> "{\"result\":\"success\",\"id\":\"99\"}";
                case "/account/7/stop/" -> "{\"result\":\"success\"}";
                default -> throw new AssertionError(path);
            };
            if (path.endsWith("/start/") || path.endsWith("/stop/")) {
                assertEquals("POST", exchange.getRequestMethod());
                assertEquals("{\"feature\":\"12\"}",
                        new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            } else {
                assertEquals("GET", exchange.getRequestMethod());
                assertEquals(0, exchange.getRequestBody().readAllBytes().length);
            }
            respond(exchange, 200, result);
        });
        FeatureApiClient client = client(Duration.ofSeconds(2));
        assertNotNull(client.validateUser("7"));
        assertEquals("12", client.getWorkItem("12").id());
        assertEquals("99", client.startTime("7", "12"));
        client.stopTime("7", "12");
    }

    @Test
    void rejectsMissingCredentialsBeforeSending() {
        FeatureApiClient client = new FeatureApiClient(new FeatureApiProperties());
        assertEquals(IntegrationFailure.AUTHENTICATION,
                assertThrows(IntegrationException.class, () -> client.getActivities()).getFailure());
    }

    @Test
    void mapsForbiddenWithoutLeakingDetails() throws Exception {
        server = server(exchange -> respond(exchange, 403, "secret material"));
        IntegrationException failure = assertThrows(IntegrationException.class,
                () -> client(Duration.ofSeconds(2)).getActivities());
        assertEquals(IntegrationFailure.AUTHENTICATION, failure.getFailure());
        assertEquals("PM authentication failed", failure.getMessage());
    }

    @Test
    void mapsMalformedResponses() throws Exception {
        server = server(exchange -> respond(exchange, 200, "{broken"));
        assertEquals(IntegrationFailure.UNAVAILABLE, assertThrows(IntegrationException.class,
                () -> client(Duration.ofSeconds(2)).getActivities()).getFailure());
    }

    private FeatureApiClient client(Duration timeout) {
        FeatureApiProperties properties = new FeatureApiProperties();
        properties.setApiServer("http://127.0.0.1:" + server.getAddress().getPort() + "/");
        properties.setAuthId("abc123"); properties.setAuthKey("secret-key"); properties.setRequestTimeout(timeout);
        IronChannelRequestSigner signer = new IronChannelRequestSigner("abc123", "secret-key",
                Clock.fixed(Instant.parse("2026-08-11T10:15:30Z"), ZoneOffset.UTC));
        return new FeatureApiClient(properties, HttpClient.newHttpClient(), signer, new Gson());
    }

    private HttpServer server(ThrowingHandler handler) throws IOException {
        HttpServer httpServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        httpServer.createContext("/", exchange -> {
            try {
                byte[] body = exchange.getRequestBody().readAllBytes();
                IronChannelRequestSignerTest.verify(exchange.getRequestHeaders().getFirst("Authorization"),
                        "secret-key", "abc123", exchange.getRequestURI().toString().substring(1), body,
                        Instant.parse("2026-08-11T10:15:30Z").getEpochSecond());
                exchange.setStreams(new java.io.ByteArrayInputStream(body), null);
                handler.handle(exchange);
            } catch (AssertionError failure) {
                serverFailure.set(failure);
                exchange.close();
            } catch (Exception failure) {
                exchange.close();
            }
        });
        httpServer.start();
        return httpServer;
    }

    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    @FunctionalInterface
    private interface ThrowingHandler { void handle(HttpExchange exchange) throws Exception; }
}
