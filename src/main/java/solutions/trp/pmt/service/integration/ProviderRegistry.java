package solutions.trp.pmt.service.integration;

import org.springframework.stereotype.Component;
import solutions.trp.pmt.integration.IntegrationException;
import solutions.trp.pmt.integration.IntegrationFailure;
import solutions.trp.pmt.integration.ProjectManagementProvider;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ProviderRegistry {
    private final Map<String, ProjectManagementProvider> providers;

    public ProviderRegistry(java.util.List<ProjectManagementProvider> providers) {
        this.providers = providers.stream().collect(Collectors.toUnmodifiableMap(ProjectManagementProvider::key, Function.identity()));
    }

    public ProjectManagementProvider get(String key) {
        ProjectManagementProvider provider = providers.get(key);
        if (provider == null) throw new IntegrationException(IntegrationFailure.UNAVAILABLE, "PM provider is unavailable");
        return provider;
    }
}
