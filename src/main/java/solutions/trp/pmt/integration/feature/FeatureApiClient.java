package solutions.trp.pmt.integration.feature;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import solutions.trp.pmt.integration.*;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class FeatureApiClient implements ProjectManagementProvider {
    public static final String PROVIDER_KEY = "feature-system";
    private static final DateTimeFormatter REMOTE_DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final FeatureApiProperties properties;
    private final HttpClient httpClient;
    private final HmacRequestSigner signer;
    private final Gson gson;

    @Autowired
    public FeatureApiClient(FeatureApiProperties properties) {
        this(properties,
                HttpClient.newBuilder().connectTimeout(properties.getConnectTimeout()).build(),
                new HmacRequestSigner(properties.getAuthId(), properties.getAuthKey(), Clock.systemUTC(), () -> {
                    byte[] nonce = new byte[8];
                    new SecureRandom().nextBytes(nonce);
                    return nonce;
                }),
                new Gson());
    }

    public FeatureApiClient(FeatureApiProperties properties, HttpClient httpClient, HmacRequestSigner signer, Gson gson) {
        this.properties = properties;
        this.httpClient = httpClient;
        this.signer = signer;
        this.gson = gson;
    }

    @Override
    public String key() { return PROVIDER_KEY; }

    @Override
    public List<ExternalWorkItem> listWorkItems(String scope) {
        JsonObject body = new JsonObject();
        body.addProperty("release", scope);
        JsonObject response = requestCollection("POST", "feature/list/", gson.toJson(body));
        List<ExternalWorkItem> items = new ArrayList<>();
        for (Map.Entry<String, JsonElement> entry : response.entrySet()) {
            items.add(toWorkItem(entry.getKey(), entry.getValue().getAsJsonObject(), false));
        }
        return items;
    }

    @Override
    public ExternalWorkItem getWorkItem(String externalId) {
        JsonObject response = request("GET", "feature/" + pathId(externalId, "feature") + "/info/", "", IntegrationFailure.NOT_FOUND);
        if (response.size() == 0) throw new IntegrationException(IntegrationFailure.NOT_FOUND, "PM task was not found");
        return toWorkItem(externalId, response, true);
    }

    @Override
    public ExternalUserProfile validateUser(String remoteAccountId) {
        JsonObject response = request("GET", "account/" + pathId(remoteAccountId, "account") + "/profile/", "", IntegrationFailure.INVALID_ACCOUNT);
        String name = string(response, "name");
        if (name == null || name.isBlank()) {
            throw new IntegrationException(IntegrationFailure.INVALID_ACCOUNT, "PM user ID is not valid");
        }
        return new ExternalUserProfile(remoteAccountId, string(response, "source"), name);
    }

    @Override
    public List<ExternalActivity> getActivities() {
        return activities(requestCollection("GET", "account/activity/", ""), null);
    }

    @Override
    public List<ExternalActivity> getActivities(String remoteAccountId) {
        String accountId = pathId(remoteAccountId, "account");
        return activities(requestCollection("GET", "account/" + accountId + "/activity/", ""), accountId);
    }

    @Override
    public List<ExternalActivity> getWorkItemActivities(String externalWorkItemId) {
        String workItemId = pathId(externalWorkItemId, "feature");
        JsonObject response = requestCollection("GET", "feature/" + workItemId + "/time/", "");
        List<ExternalActivity> activities = new ArrayList<>();
        for (Map.Entry<String, JsonElement> entry : response.entrySet()) {
            JsonObject value = entry.getValue().getAsJsonObject();
            if (string(value, "stop") == null) {
                activities.add(new ExternalActivity(entry.getKey(), workItemId,
                        string(value, "account_id"), dateTime(value, "start")));
            }
        }
        return activities;
    }

    private List<ExternalActivity> activities(JsonObject response, String fallbackAccountId) {
        List<ExternalActivity> activities = new ArrayList<>();
        for (Map.Entry<String, JsonElement> entry : response.entrySet()) {
            JsonObject value = entry.getValue().getAsJsonObject();
            String accountId = string(value, "account_id");
            activities.add(new ExternalActivity(entry.getKey(), string(value, "feature"),
                    accountId == null ? fallbackAccountId : accountId, dateTime(value, "start")));
        }
        return activities;
    }

    @Override
    public String startTime(String remoteAccountId, String externalWorkItemId) {
        JsonObject body = new JsonObject();
        body.addProperty("feature", externalWorkItemId);
        JsonObject response = request("POST", "account/" + pathId(remoteAccountId, "account") + "/start/", gson.toJson(body));
        if (!"success".equals(string(response, "result")) || string(response, "id") == null) {
            throw new IntegrationException(IntegrationFailure.UNAVAILABLE, "PM did not confirm that the timer started");
        }
        return string(response, "id");
    }

    @Override
    public void stopTime(String remoteAccountId, String externalWorkItemId) {
        JsonObject body = new JsonObject();
        body.addProperty("feature", externalWorkItemId);
        JsonObject response = request("POST", "account/" + pathId(remoteAccountId, "account") + "/stop/", gson.toJson(body));
        if (!"success".equals(string(response, "result"))) {
            throw new IntegrationException(IntegrationFailure.UNAVAILABLE, "PM did not confirm that the timer stopped");
        }
    }

    @Override
    public boolean timeEntryExists(String remoteAccountId, String externalWorkItemId,
                                   String remoteRegistrationId) {
        String accountId = pathId(remoteAccountId, "account");
        String workItemId = pathId(externalWorkItemId, "feature");
        String registrationId = pathId(remoteRegistrationId, "time registration");
        JsonObject response = requestCollection("GET", "feature/" + workItemId + "/time/", "");
        JsonElement registration = response.get(registrationId);
        if (registration == null || !registration.isJsonObject()) return false;
        return accountId.equals(string(registration.getAsJsonObject(), "account_id"));
    }

    @Override
    public ExternalTimeUpdate updateTime(String remoteAccountId, String externalWorkItemId,
                                         String remoteRegistrationId, Instant start, Instant end) {
        String accountId = pathId(remoteAccountId, "account");
        String workItemId = pathId(externalWorkItemId, "feature");
        String registrationId = pathId(remoteRegistrationId, "time registration");
        if (start == null || end == null) {
            throw new IntegrationException(IntegrationFailure.BAD_REQUEST, "PM time timestamps are required");
        }

        JsonObject body = new JsonObject();
        body.addProperty("feature", workItemId);
        body.addProperty("time", registrationId);
        body.addProperty("start", remoteDateTime(start));
        body.addProperty("stop", remoteDateTime(end));
        JsonObject response = request("POST", "account/" + accountId + "/time/", gson.toJson(body));
        if (!"success".equals(string(response, "result"))) {
            throw new IntegrationException(IntegrationFailure.UNAVAILABLE, "PM did not confirm that the time entry was updated");
        }

        Instant previousStart = instant(response, "start_previous");
        Instant previousEnd = instant(response, "stop_previous");
        Instant confirmedStart = instant(response, "start");
        Instant confirmedEnd = instant(response, "stop");
        if (previousStart == null || previousEnd == null || confirmedStart == null || confirmedEnd == null) {
            throw new IntegrationException(IntegrationFailure.UNAVAILABLE, "PM returned an invalid time update response");
        }
        return new ExternalTimeUpdate(previousStart, previousEnd, confirmedStart, confirmedEnd);
    }

    private JsonObject request(String method, String endpoint, String body) {
        return request(method, endpoint, body, IntegrationFailure.UNAVAILABLE, false);
    }

    private JsonObject request(String method, String endpoint, String body, IntegrationFailure nullFailure) {
        return request(method, endpoint, body, nullFailure, false);
    }

    private JsonObject requestCollection(String method, String endpoint, String body) {
        return request(method, endpoint, body, IntegrationFailure.UNAVAILABLE, true);
    }

    private JsonObject request(String method, String endpoint, String body, IntegrationFailure nullFailure,
                               boolean allowEmptyArray) {
        ensureConfigured();
        HttpRequest.Builder builder = HttpRequest.newBuilder(resolve(endpoint))
                .timeout(properties.getRequestTimeout())
                .header("Content-Type", "application/json")
                .header("Authorization", signer.authorization(method, endpoint, body));
        builder.method(method, body.isEmpty()
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body));
        try {
            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw responseError(response.statusCode(), response.body());
            }
            JsonElement parsed = gson.fromJson(response.body(), JsonElement.class);
            if (parsed == null || parsed.isJsonNull()) {
                String message = nullFailure == IntegrationFailure.INVALID_ACCOUNT ? "PM user ID is not valid"
                        : nullFailure == IntegrationFailure.NOT_FOUND ? "PM resource was not found" : "PM returned an invalid response";
                throw new IntegrationException(nullFailure, message);
            }
            if (allowEmptyArray && parsed.isJsonArray() && parsed.getAsJsonArray().isEmpty()) {
                return new JsonObject();
            }
            if (!parsed.isJsonObject()) {
                throw new IntegrationException(IntegrationFailure.UNAVAILABLE, "PM returned an invalid response");
            }
            JsonObject object = parsed.getAsJsonObject();
            if (object.has("error")) throw responseError(500, response.body());
            return object;
        } catch (java.net.http.HttpTimeoutException e) {
            throw new IntegrationException(IntegrationFailure.TIMEOUT, "PM request timed out", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IntegrationException(IntegrationFailure.UNAVAILABLE, "PM request was interrupted", e);
        } catch (IOException | RuntimeException e) {
            if (e instanceof IntegrationException integrationException) throw integrationException;
            throw new IntegrationException(IntegrationFailure.UNAVAILABLE, "PM service is unavailable", e);
        }
    }

    private IntegrationException responseError(int status, String responseBody) {
        IntegrationFailure failure = switch (status) {
            case 400 -> IntegrationFailure.BAD_REQUEST;
            case 401, 403 -> IntegrationFailure.AUTHENTICATION;
            case 404 -> IntegrationFailure.NOT_FOUND;
            default -> IntegrationFailure.UNAVAILABLE;
        };
        String safeMessage = switch (failure) {
            case AUTHENTICATION -> "PM authentication failed";
            case BAD_REQUEST -> "PM rejected the request";
            case NOT_FOUND -> "PM resource was not found";
            default -> "PM service is unavailable";
        };
        return new IntegrationException(failure, safeMessage);
    }

    private ExternalWorkItem toWorkItem(String fallbackId, JsonObject value, boolean details) {
        String id = string(value, "id");
        if (id == null) id = fallbackId;
        Map<String, String> metadata = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> field : value.entrySet()) {
            if (!field.getValue().isJsonNull() && field.getValue().isJsonPrimitive()) {
                metadata.put(field.getKey(), field.getValue().getAsString());
            }
        }
        return new ExternalWorkItem(id, string(value, "header"), string(value, "status"),
                string(value, "release"), string(value, "module"), string(value, "type"),
                string(value, "account_id"), dateTime(value, "time_create"),
                details ? string(value, "description") : null,
                details ? string(value, "devnotes") : null, metadata);
    }

    private URI resolve(String endpoint) {
        String root = properties.getApiServer().trim();
        if (!root.endsWith("/")) root += "/";
        return URI.create(root + endpoint);
    }

    private void ensureConfigured() {
        if (properties.getApiServer() == null || properties.getApiServer().isBlank()
                || properties.getAuthId() == null || properties.getAuthId().isBlank()
                || properties.getAuthKey() == null || properties.getAuthKey().isBlank()) {
            throw new IntegrationException(IntegrationFailure.AUTHENTICATION, "PM integration is not configured");
        }
    }

    private static String pathId(String id, String kind) {
        if (id == null || !id.matches("[1-9][0-9]*")) {
            throw new IntegrationException(kind.equals("account") ? IntegrationFailure.INVALID_ACCOUNT : IntegrationFailure.BAD_REQUEST,
                    "Invalid PM " + kind + " ID");
        }
        return id;
    }

    private static String string(JsonObject object, String name) {
        JsonElement value = object.get(name);
        return value == null || value.isJsonNull() ? null : value.getAsString();
    }

    private static LocalDateTime dateTime(JsonObject object, String name) {
        String value = string(object, name);
        if (value == null) return null;
        try { return LocalDateTime.parse(value, REMOTE_DATE_TIME); }
        catch (DateTimeParseException ignored) { return null; }
    }

    private static String remoteDateTime(Instant value) {
        return REMOTE_DATE_TIME.format(value.atZone(ZoneId.systemDefault()));
    }

    private static Instant instant(JsonObject object, String name) {
        LocalDateTime value = dateTime(object, name);
        return value == null ? null : value.atZone(ZoneId.systemDefault()).toInstant();
    }
}
