package solutions.trp.pmt.controller.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import solutions.trp.pmt.controller.api.response.ApiResponse;
import solutions.trp.pmt.service.TimeService;

import java.util.List;

@RestController
@RequestMapping("/api/time")
public class TimeController {
    private final TimeService timeService;

    @Autowired
    public TimeController(TimeService timeService) {
        this.timeService = timeService;
    }

//    GET /time-entries/summary?taskId=1
//    GET /time-entries/summary?taskId=1&userIds=2,3
//    GET /time-entries/summary?projectId=5
//    GET /time-entries/summary?projectId=5&userIds=2,3
//    GET /time-entries/summary?userIds=2,3
    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<Long>> getTimeSummary(
            @RequestParam(required = false) Integer taskId,
            @RequestParam(required = false) Integer projectId,
            @RequestParam(required = false) List<Integer> userIds
    ) {
        long totalTime = timeService.calculateTime(taskId, projectId, userIds);
        return ResponseEntity.ok(ApiResponse.ok(totalTime));
    }
}
