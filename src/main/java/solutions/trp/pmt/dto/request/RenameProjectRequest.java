package solutions.trp.pmt.dto.request;

import jakarta.validation.constraints.NotBlank;

public record RenameProjectRequest(
        @NotBlank String title,
        int id
){

}
