package solutions.trp.pmt.controller.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import solutions.trp.pmt.controller.api.response.ApiResponse;
import solutions.trp.pmt.datasource.users.UserEntity;
import solutions.trp.pmt.dto.DashboardDto;
import solutions.trp.pmt.dto.ProjectDto;
import solutions.trp.pmt.dto.UserDto;
import solutions.trp.pmt.service.JsonPaper;

import java.util.List;

@RestController
@RequestMapping("/api/public/jsonpaper")
public class JsonPaperController {
    private final JsonPaper jsonPaper;

    public JsonPaperController(JsonPaper jsonPaper) {
        this.jsonPaper = jsonPaper;
    }

    @GetMapping
    public ResponseEntity<String> getDashboard() {
        return ResponseEntity.ok(jsonPaper.createDisplay());
    }
}
