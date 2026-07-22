package solutions.trp.pmt.controller.frontend;

import com.google.gson.Gson;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.ui.Model;
import solutions.trp.pmt.datasource.users.UserEntity;
import solutions.trp.pmt.service.AppUserDetailsService;
import solutions.trp.pmt.service.FrontendAssetBundleService;

@Controller
public class PageController {

    private final ResourceLoader resourceLoader;
    private final FrontendAssetBundleService frontendAssetBundleService;
    private final AppUserDetailsService appUserDetailsService;
    private final Gson gson = new Gson();

    public PageController(
            ResourceLoader resourceLoader,
            FrontendAssetBundleService frontendAssetBundleService,
            AppUserDetailsService appUserDetailsService
    ) {
        this.resourceLoader = resourceLoader;
        this.frontendAssetBundleService = frontendAssetBundleService;
        this.appUserDetailsService = appUserDetailsService;
    }

    @GetMapping("/login")
    public String loginPage(HttpServletRequest request, Model model) {
        addFrontendModel(request, model, "login");
        return "frontend/page/login";
    }

    @GetMapping(value = "/**")
    public String rootPage(HttpServletRequest request, Model model) {
        String uri = request.getServletPath();
        if (uri.startsWith("/")) {
            uri = uri.substring(1);
        }

        if (uri.endsWith(".html")) uri = uri.substring(0, uri.length() - 5);


        String path = "classpath:/frontend/page/" + uri + ".html";
        Resource resource = resourceLoader.getResource(path);

        if (resource.exists()) {
            addFrontendModel(request, model, uri);
            return "frontend/page/" + uri;
        } else {
            return "handler/404";
        }
    }

    @GetMapping("/")
    public String index(HttpServletRequest request, Model model) {
        addFrontendModel(request, model, "index");
        return "frontend/page/index";
    }

    private void addFrontendModel(HttpServletRequest request, Model model, String pageName) {
        String scriptPath = frontendAssetBundleService.scriptPath(pageName);
        model.addAttribute("scriptBundle", scriptPath == null ? null : request.getContextPath() + scriptPath);
        model.addAttribute("currentUserJson", currentUserJson());
    }

    private String currentUserJson() {
        try {
            UserEntity user = appUserDetailsService.getUserEntity();
            return gson.toJson(user.toDto());
        } catch (RuntimeException e) {
            return "null";
        }
    }
}
