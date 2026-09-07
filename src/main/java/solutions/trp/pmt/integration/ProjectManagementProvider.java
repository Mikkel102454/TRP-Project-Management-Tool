package solutions.trp.pmt.integration;

import java.time.Instant;
import java.util.List;

/** Provider-neutral boundary for project-management integrations. */
public interface ProjectManagementProvider {
    String key();

    List<ExternalWorkItem> listWorkItems(String scope);

    ExternalWorkItem getWorkItem(String externalId);

    ExternalUserProfile validateUser(String remoteAccountId);

    List<ExternalActivity> getActivities();

    List<ExternalActivity> getActivities(String remoteAccountId);

    List<ExternalActivity> getWorkItemActivities(String externalWorkItemId);

    String startTime(String remoteAccountId, String externalWorkItemId);

    void stopTime(String remoteAccountId, String externalWorkItemId);

    boolean timeEntryExists(String remoteAccountId, String externalWorkItemId,
                            String remoteRegistrationId);

    ExternalTimeUpdate updateTime(String remoteAccountId, String externalWorkItemId,
                                  String remoteRegistrationId, Instant start, Instant end);
}
