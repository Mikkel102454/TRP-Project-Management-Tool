package solutions.trp.pmt.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import solutions.trp.pmt.controller.api.execption.BadRequestException;
import solutions.trp.pmt.controller.api.execption.ConflictException;
import solutions.trp.pmt.controller.api.execption.NotFoundException;
import solutions.trp.pmt.controller.api.execption.UnauthorizedException;
import solutions.trp.pmt.datasource.users.UserEntity;
import solutions.trp.pmt.datasource.users.UserRepository;
import solutions.trp.pmt.util.PasswordEncoding;

import java.util.List;
import java.util.Objects;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final AppUserDetailsService appUserDetailsService;

    @Autowired
    public UserService(UserRepository userRepository, AppUserDetailsService appUserDetailsService) {
        this.userRepository = userRepository;
        this.appUserDetailsService = appUserDetailsService;
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
    public int addUser(String username, String initial, String password, String email, boolean admin, boolean enabled) {
        if (userRepository.existsByUsername(username)) throw new ConflictException("Username already in use");
        if (password.length() < 3) throw new BadRequestException("Password must be at least 3 characters");
        if (username.length() < 3) throw new BadRequestException("Username must be at least 3 characters");
        String normalizedEmail = normalizeEmail(email);
        if(normalizedEmail != null && userRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new ConflictException("Email already in use");
        }

        UserEntity user = new UserEntity();
        user.setUsername(username);
        user.setInitial(initial);
        user.setEmail(normalizedEmail);
        user.setPassword(PasswordEncoding.encode("bcrypt", password));
        user.setAdmin(admin);
        user.setEnabled(enabled);

        userRepository.save(user);

        return 0;
    }

    public void updateUser(int userId, String username, String initial, String password, String email, Boolean admin, Boolean enabled) {
        UserEntity user = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));

        if(username != null && !username.isBlank() && !Objects.equals(user.getUsername(), username)) {
            user.setUsername(username);
        }

        if(initial != null && !initial.isBlank() && !Objects.equals(user.getInitial(), initial)) {
            user.setInitial(initial);
        }

        if(password != null && !password.isBlank()) {
            user.setPassword(PasswordEncoding.encode("bcrypt", password));
        }

        if(email != null) {
            String normalizedEmail = normalizeEmail(email);
            if(normalizedEmail != null) {
                userRepository.findByEmail(normalizedEmail)
                        .filter(existingUser -> existingUser.getId() != user.getId())
                        .ifPresent(existingUser -> {
                            throw new ConflictException("Email already in use");
                        });
            }
            user.setEmail(normalizedEmail);
        }

        if(admin != null) {
            user.setAdmin(admin);
        }

        if(enabled != null) {
            user.setEnabled(enabled);
        }

        userRepository.save(user);
    }

    public UserEntity getCurrentUser(){
        return appUserDetailsService.getUserEntity();
    }

    public List<UserEntity> getAllUsers() {
        return userRepository.findAll();
    }

    public void updatePassword(String oldPassword, String newPassword){
        UserEntity user = getCurrentUser();
        if(!PasswordEncoding.matches("bcrypt", oldPassword, user.getPassword())) {
            throw new UnauthorizedException("Password is not correct");
        }
        user.setPassword(PasswordEncoding.encode("bcrypt", newPassword));
        userRepository.save(user);
    }

    public void updatePassword(UserEntity user, String newPassword) {
        if(newPassword == null || newPassword.length() < 3) {
            throw new BadRequestException("Password must be at least 3 characters");
        }
        user.setPassword(PasswordEncoding.encode("bcrypt", newPassword));
        userRepository.save(user);
    }

    private String normalizeEmail(String email) {
        if(email == null || email.isBlank()) {
            return null;
        }
        return email.trim().toLowerCase();
    }
}
