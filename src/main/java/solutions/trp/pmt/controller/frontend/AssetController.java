package solutions.trp.pmt.controller.frontend;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import solutions.trp.pmt.service.FrontendAssetBundleService;

import java.util.concurrent.TimeUnit;

@RestController
public class AssetController {
    private final FrontendAssetBundleService frontendAssetBundleService;

    public AssetController(FrontendAssetBundleService frontendAssetBundleService) {
        this.frontendAssetBundleService = frontendAssetBundleService;
    }

    @GetMapping(value = "/assets/{pageName}.{hash}.js", produces = "text/javascript")
    public ResponseEntity<String> scriptBundle(
            @PathVariable String pageName,
            @PathVariable String hash
    ) {
        FrontendAssetBundleService.Bundle bundle = frontendAssetBundleService.getBundle(pageName, hash);

        if (bundle == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        return ResponseEntity.ok()
                .contentType(MediaType.valueOf("text/javascript"))
                .cacheControl(CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic().immutable())
                .header(HttpHeaders.VARY, "Accept-Encoding")
                .body(bundle.content());
    }
}
