package solutions.trp.pmt.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreateUserRequest(
        @NotBlank String username,
        @NotBlank String password,
        @NotBlank String initial,
        String email,
        Boolean isAdmin,
        Boolean isEnabled
){

}
