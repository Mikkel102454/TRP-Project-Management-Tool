package solutions.trp.pmt.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

public record UpdateTaskRequest(
    Integer taskId,
    @NotBlank String title,
    Boolean isCompleted,
    LocalDateTime deadline,
    Integer estimatedTime,
    String description,
    String status
){

}
