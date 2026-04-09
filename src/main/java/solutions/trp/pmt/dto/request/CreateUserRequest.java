package solutions.trp.pmt.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreateUserRequest(
        @NotBlank String username,
        @NotBlank String password,
        Boolean isAdmin,
        Boolean isEnabled
){

}
