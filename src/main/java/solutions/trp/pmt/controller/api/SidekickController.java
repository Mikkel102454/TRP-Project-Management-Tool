package solutions.trp.pmt.controller.api;

import jakarta.servlet.http.HttpServletRequest;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import solutions.trp.pmt.controller.api.response.ApiResponse;
import solutions.trp.pmt.service.SidekickService;
import solutions.trp.pmt.service.SidekickVideoService;

import java.io.IOException;
import java.time.Instant;
import solutions.trp.pmt.controller.api.response.ApiError;
import java.util.List;

@RestController
public class SidekickController {
    private final SidekickService service;

    private final SidekickVideoService videos;

    public SidekickController(SidekickService service, SidekickVideoService videos) {
        this.service = service;
        this.videos = videos;
    }

    public record Info(String video, String downloadUrl, String checksum, List<SidekickService.Overlay> overlays) {}

    public record InfoResponse(boolean success, String serverTime, Info data, ApiError error) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ButtonAction(String action, String type, String url) {}

    public record Endpoints(String page, String info,
                            ButtonAction button1Pressed, ButtonAction button1Held, ButtonAction button1DoubleClicked,
                            ButtonAction button2Pressed, ButtonAction button2Held, ButtonAction button2DoubleClicked,
                            ButtonAction bothButtonsPressed, ButtonAction bothButtonsHeld) {}

    public record Config(Endpoints endpoints) {}

    @GetMapping("/api/gadget/sidekick/config")
    public ResponseEntity<ApiResponse<Config>> config(HttpServletRequest request) {
        String context = request.getContextPath();
        Config config = new Config(new Endpoints(context + "/gadget/sidekick/display",
                context + "/api/gadget/sidekick",
                new ButtonAction("request", "POST", context + "/api/gadget/sidekick/button1/pressed"),
                new ButtonAction("request", "POST", context + "/api/gadget/sidekick/button1/held"),
                null,
                new ButtonAction("request", "POST", context + "/api/gadget/sidekick/button2/pressed"),
                new ButtonAction("request", "POST", context + "/api/gadget/sidekick/button2/held"),
                null, new ButtonAction("toggleWindow", null, null), null));
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ApiResponse.ok(config));
    }

    @PostMapping("/api/gadget/sidekick/button1/pressed")
    public ResponseEntity<ApiResponse<Void>> button1Pressed() {
        service.button1Pressed();
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ApiResponse.ok());
    }

    @PostMapping("/api/gadget/sidekick/button1/held")
    public ResponseEntity<ApiResponse<Void>> button1Held() {
        service.button1Held();
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ApiResponse.ok());
    }

    @PostMapping("/api/gadget/sidekick/button2/pressed")
    public ResponseEntity<ApiResponse<Void>> button2Pressed() {
        service.button2Pressed();
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ApiResponse.ok());
    }

    @PostMapping("/api/gadget/sidekick/button2/held")
    public ResponseEntity<ApiResponse<Void>> button2Held() {
        service.button2Held();
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ApiResponse.ok());
    }

    @GetMapping("/api/gadget/sidekick")
    public ResponseEntity<InfoResponse> info(HttpServletRequest request) throws IOException {
        SidekickService.Info state = service.info();
        String downloadUrl = request.getContextPath() + "/api/gadget/sidekick/videos/" + state.video();
        Info info = new Info(state.video(), downloadUrl, videos.checksum(state.video()), state.overlays());
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(new InfoResponse(true, Instant.now().toString(), info, null));
    }

    @GetMapping("/api/gadget/sidekick/videos/{filename}")
    public ResponseEntity<Resource> video(@PathVariable String filename) throws IOException {
        Resource resource = videos.get(filename);
        if (resource == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("video/mp4"))
                .contentLength(resource.contentLength())
                .cacheControl(CacheControl.noCache().cachePrivate())
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(filename).build().toString())
                .body(resource);
    }

    @GetMapping("/gadget/sidekick/data")
    public ResponseEntity<ApiResponse<SidekickService.Display>> data() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ApiResponse.ok(service.display()));
    }
}
