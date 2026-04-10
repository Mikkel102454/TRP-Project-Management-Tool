package solutions.trp.pmt.controller.frontend;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
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

    @Cacheable("resources")
    @GetMapping("/{page}")
    public String rootPage(@PathVariable String page) {
        if (page.endsWith(".html")) page = page.substring(0, page.length() - 5);

        String path = "classpath:/frontend/page/" + page + ".html";
        Resource resource = resourceLoader.getResource(path);

        if (resource.exists()) {
            return "frontend/page/" + page;
        }

        path = "classpath:/frontend/page/" + page + ".php";
        resource = resourceLoader.getResource(path);

        if (resource.exists()) {
            return "frontend/page/" + page;
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
