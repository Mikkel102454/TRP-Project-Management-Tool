package solutions.trp.pmt;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import solutions.trp.pmt.datasource.users.UserRepository;
import solutions.trp.pmt.service.UserService;
import java.util.TimeZone;

@Component
public class Start implements CommandLineRunner {
    private final UserService userService;
    private final UserRepository userRepository;

    @Autowired
    public Start(UserService userService, UserRepository userRepository) {
        this.userService = userService;
        this.userRepository = userRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

        if(!userRepository.existsByAdminTrue()){
            userService.addUser("admin", "ad", "admin", true, true);
        }
    }
}
