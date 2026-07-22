package solutions.trp.pmt.service;

import com.google.gson.Gson;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class FrontendAssetBundleService {
    private static final Pattern SCRIPT_PATTERN = Pattern.compile("<script\\s+[^>]*th:src=\"@\\{/code/([^\"}]+)}\"[^>]*></script>");
    private static final String PAGE_PATTERN = "classpath*:frontend/page/*.html";
    private static final String COMPONENT_PATTERN = "classpath*:frontend/page/components/*.html";
    private static final Map<String, List<String>> SCRIPT_MANIFEST = Map.of(
            "login", List.of(
                    "consts.js", "pages/login.js"
            ),
            "reset-password", List.of(
                    "consts.js", "pages/reset-password.js"
            ),
            "index", List.of(
                    "util/led.js", "util/logger.js", "util/components.js", "util/avatar.js", "util/user.js",
                    "util/string.js", "consts.js", "dto/User.js", "dto/Task.js", "dto/Project.js",
                    "util/header.js", "util/page.js", "network/ProjectNet.js", "network/TaskNet.js", "network/UserNet.js",
                    "pages/index.js"
            ),
            "archives", List.of(
                    "util/led.js", "util/logger.js", "util/components.js", "util/avatar.js", "util/user.js",
                    "util/string.js", "consts.js", "dto/User.js", "dto/Task.js", "dto/Project.js",
                    "util/header.js", "util/page.js", "network/ProjectNet.js", "network/TaskNet.js", "network/UserNet.js",
                    "pages/archives.js"
            ),
            "task", List.of(
                    "util/led.js", "util/status.js", "util/logger.js", "util/components.js", "util/avatar.js",
                    "util/time.js", "util/user.js", "util/string.js", "consts.js", "dto/User.js", "dto/Task.js",
                    "dto/Project.js", "util/header.js", "util/page.js", "network/ProjectNet.js", "network/TaskNet.js",
                    "pages/task.js"
            ),
            "setup", List.of(
                    "util/led.js", "util/logger.js", "util/components.js", "util/avatar.js", "util/user.js",
                    "consts.js", "network/UserNet.js", "dto/User.js", "util/header.js", "util/page.js",
                    "pages/setup.js"
            ),
            "time", List.of(
                    "util/logger.js", "util/components.js", "util/avatar.js", "util/user.js", "consts.js",
                    "network/UserNet.js", "dto/User.js", "util/header.js", "util/page.js", "pages/time.js"
            ),
            "timetable", List.of(
                    "util/logger.js", "util/components.js", "util/avatar.js", "util/user.js", "consts.js",
                    "network/UserNet.js", "network/ProjectNet.js", "network/TimeNet.js", "dto/User.js",
                    "dto/Project.js", "dto/Task.js", "dto/Time.js", "util/header.js", "util/page.js",
                    "pages/timetable.js"
            )
    );

    private final ResourcePatternResolver resourcePatternResolver;
    private final Gson gson = new Gson();
    private final Map<String, Bundle> bundles = new HashMap<>();

    public FrontendAssetBundleService(ResourcePatternResolver resourcePatternResolver) {
        this.resourcePatternResolver = resourcePatternResolver;
    }

    @PostConstruct
    public void buildBundles() throws IOException {
        Map<String, String> componentTemplates = loadComponentTemplates();
        String componentPrelude = "window.__COMPONENT_TEMPLATES__ = "
                + gson.toJson(componentTemplates)
                + ";\n";

        for (Resource page : resourcePatternResolver.getResources(PAGE_PATTERN)) {
            String pageName = filenameWithoutExtension(page.getFilename());
            String pageHtml = read(page);
            StringBuilder bundle = new StringBuilder(componentPrelude);

            for (String scriptPath : scriptPaths(pageName, pageHtml)) {
                appendScript(bundle, scriptPath);
            }

            String content = bundle.toString();
            String hash = sha256(content).substring(0, 12);
            bundles.put(pageName, new Bundle(pageName, hash, content));
        }
    }

    private List<String> scriptPaths(String pageName, String pageHtml) {
        List<String> manifestScripts = SCRIPT_MANIFEST.get(pageName);
        if (manifestScripts != null) {
            return manifestScripts;
        }

        Matcher matcher = SCRIPT_PATTERN.matcher(pageHtml);
        java.util.ArrayList<String> scriptPaths = new java.util.ArrayList<>();

        while (matcher.find()) {
            scriptPaths.add(matcher.group(1));
        }

        return scriptPaths;
    }

    private void appendScript(StringBuilder bundle, String scriptPath) throws IOException {
        Resource script = resourcePatternResolver.getResource("classpath:frontend/code/" + scriptPath);

        if (!script.exists() && !scriptPath.endsWith(".js")) {
            script = resourcePatternResolver.getResource("classpath:frontend/code/" + scriptPath + ".js");
        }

        if (!script.exists()) {
            return;
        }

        bundle.append("\n;/* ").append(scriptPath).append(" */\n");
        bundle.append(read(script));
        bundle.append("\n");
    }

    public String scriptPath(String pageName) {
        Bundle bundle = bundles.get(pageName);
        if (bundle == null) {
            return null;
        }

        return "/assets/" + bundle.pageName() + "." + bundle.hash() + ".js";
    }

    public Bundle getBundle(String pageName, String hash) {
        Bundle bundle = bundles.get(pageName);
        if (bundle == null || !bundle.hash().equals(hash)) {
            return null;
        }

        return bundle;
    }

    private Map<String, String> loadComponentTemplates() throws IOException {
        Map<String, String> templates = new LinkedHashMap<>();

        for (Resource component : resourcePatternResolver.getResources(COMPONENT_PATTERN)) {
            templates.put(filenameWithoutExtension(component.getFilename()), read(component));
        }

        return templates;
    }

    private String read(Resource resource) throws IOException {
        try (InputStream inputStream = resource.getInputStream()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private String filenameWithoutExtension(String filename) {
        if (filename == null) {
            return "";
        }

        int index = filename.lastIndexOf('.');
        return index == -1 ? filename : filename.substring(0, index);
    }

    private String sha256(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(bytes.length * 2);

            for (byte b : bytes) {
                hex.append(String.format("%02x", b));
            }

            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    public record Bundle(String pageName, String hash, String content) {
    }
}
