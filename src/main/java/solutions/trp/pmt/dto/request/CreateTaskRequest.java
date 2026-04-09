package solutions.trp.pmt.dto.request;

import java.time.LocalDateTime;

public record CreateTaskRequest(
    String title,
    Boolean isCompleted,
    LocalDateTime deadline,
    Integer estimatedTime,
    String description
){

}
