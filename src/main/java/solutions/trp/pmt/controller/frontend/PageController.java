package solutions.trp.pmt.controller.frontend;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import solutions.trp.pmt.service.HandlerService;

@Controller
public class PageController {

    private final ResourceController resourceController;
    private final HandlerService handlerService;
    private ResourceLoader resourceLoader;

    public PageController(ResourceController resourceController, HandlerService handlerService, ResourceLoader resourceLoader) {
        this.resourceController = resourceController;
        this.handlerService = handlerService;
        this.resourceLoader = resourceLoader;
    }

    @GetMapping("/login")
    public String loginPage() {
        return "frontend/page/login";
    }

    @Cacheable("resources")
    @GetMapping(value = "/**")
    public String rootPage(HttpServletRequest request) {
        String uri = request.getServletPath();
        if (uri.startsWith("/")) {
            uri = uri.substring(1);
        }

        if (uri.endsWith(".html")) uri = uri.substring(0, uri.length() - 5);


        String path = "classpath:/frontend/page/" + uri + ".html";
        Resource resource = resourceLoader.getResource(path);

        if (resource.exists()) {
            return "frontend/page/" + uri;
        } else {
            return "handler/404";
        }
    }

    @Cacheable("resources")
    @GetMapping("/")
    public String index() {
        return "frontend/page/index";
    }
}
