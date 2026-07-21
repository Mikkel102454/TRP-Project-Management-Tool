package solutions.trp.pmt.dto.request;

public record UpdateUserRequest(
        int userId,
        String username,
        String initial,
        String password,
        String email,
        Boolean isAdmin,
        Boolean isEnabled
){

}
