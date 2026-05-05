package solutions.trp.pmt.controller.api;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import solutions.trp.pmt.controller.api.execption.UnauthorizedException;
import solutions.trp.pmt.controller.api.response.ApiResponse;
import solutions.trp.pmt.datasource.time_tables.TimingEntity;
import solutions.trp.pmt.dto.TimeDto;
import solutions.trp.pmt.dto.TimeValidationDto;
import solutions.trp.pmt.dto.UserDto;
import solutions.trp.pmt.service.AppUserDetailsService;
import solutions.trp.pmt.service.TimeService;
import org.springframework.beans.factory.annotation.Value;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/time")
public class TimeController {
    private final TimeService timeService;
    private final AppUserDetailsService appUserDetailsService;

    @Autowired
    public TimeController(TimeService timeService, AppUserDetailsService appUserDetailsService) {
        this.timeService = timeService;
        this.appUserDetailsService = appUserDetailsService;
    }

    @GetMapping("/summary/full")
    public ResponseEntity<ApiResponse<List<TimeDto>>> getAllTimeSummary() {
        return ResponseEntity.ok(ApiResponse.ok(timeService.getAllTime().stream().map(TimingEntity::toDto).toList()));
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<List<TimeDto>>> getAllTimeSummary(
            @RequestParam Integer userId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(timeService.getAllTimeByUserId(userId).stream().map(TimingEntity::toDto).toList()));
    }

    @DeleteMapping("/summary")
    public ResponseEntity<ApiResponse<List<TimeDto>>> deleteTimeSummary(
            @RequestParam Integer timeId
    ) {
        timeService.deleteTimeEntry(timeId);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @PatchMapping("/summary")
    public ResponseEntity<ApiResponse<List<TimeDto>>> updateTimeSummary(
            @RequestParam Integer timeId,
            @RequestParam OffsetDateTime startTime,
            @RequestParam OffsetDateTime endTime
    ) {
        timeService.updateTimeEntry(timeId, startTime, endTime);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @GetMapping
    public ResponseEntity<ApiResponse<TimeValidationDto>> getTime(
            @RequestParam Integer userId
    ) {
        TimeValidationDto result = timeService.getValidatedTime(userId);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}
