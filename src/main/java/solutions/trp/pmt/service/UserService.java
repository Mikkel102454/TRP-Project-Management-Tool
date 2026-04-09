package solutions.trp.pmt.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import solutions.trp.pmt.controller.api.execption.NotFoundException;
import solutions.trp.pmt.datasource.users.UserEntity;
import solutions.trp.pmt.datasource.users.UserRepository;
import solutions.trp.pmt.util.PasswordEncoding;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class UserService {
    private final UserRepository userRepository;

    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Creates user
     *
     * @param username user's username
     * @param password user's password
     * @param admin is user admin
     * @return exit code
     *
     */
    public int addUser(String username, String password, boolean admin, boolean enabled) {
        try{
            if(userRepository.existsByUsername(username)) { return 2; }
            if(password.length() < 3) { return 3; }
            if(username.length() < 3) { return 4; }

            UserEntity user = new UserEntity();
            user.setUsername(username);
            user.setPassword(PasswordEncoding.encode("bcrypt", password));
            user.setAdmin(admin);
            user.setEnabled(enabled);

            userRepository.save(user);

            return 0;
        } catch (Exception e) {
            System.err.println(e.getMessage());
            return 1;
        }
    }

    public void updateUser(int userId, String username, String password, Boolean admin, Boolean enabled) {
        UserEntity user = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));

        if(username != null && !username.isBlank() && !Objects.equals(user.getUsername(), username)) {
            user.setUsername(username);
        }

        if(password != null && !password.isBlank()) {
            user.setPassword(PasswordEncoding.encode("bcrypt", password));
        }

        if(admin != null) {
            user.setAdmin(admin);
        }

        if(enabled != null) {
            user.setEnabled(enabled);
        }

        userRepository.save(user);
    }
}
