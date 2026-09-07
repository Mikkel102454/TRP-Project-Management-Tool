package solutions.trp.pmt.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import solutions.trp.pmt.controller.api.execption.BadRequestException;
import solutions.trp.pmt.controller.api.execption.ConflictException;
import solutions.trp.pmt.controller.api.execption.NotFoundException;
import solutions.trp.pmt.controller.api.execption.UnauthorizedException;
import solutions.trp.pmt.datasource.users.UserEntity;
import solutions.trp.pmt.datasource.users.UserRepository;
import solutions.trp.pmt.util.PasswordEncoding;
import solutions.trp.pmt.service.integration.IntegrationBindingService;

import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

@Service
public class UserService {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final UserRepository userRepository;
    private final AppUserDetailsService appUserDetailsService;
    private final IntegrationBindingService integrationBindingService;

    public UserService(UserRepository userRepository, AppUserDetailsService appUserDetailsService) {
        this(userRepository, appUserDetailsService, null);
    }

    @Autowired
    public UserService(UserRepository userRepository, AppUserDetailsService appUserDetailsService,
                       IntegrationBindingService integrationBindingService) {
        this.userRepository = userRepository;
        this.appUserDetailsService = appUserDetailsService;
        this.integrationBindingService = integrationBindingService;
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
        return addUser(username, initial, password, email, admin, enabled, null);
    }

    @Transactional
    public int addUser(String username, String initial, String password, String email, boolean admin, boolean enabled,
                       String pmUserId) {
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
        if (pmUserId != null && !pmUserId.isBlank()) requireIntegrationBindings().setUserBinding(user, pmUserId);

        return 0;
    }

    public void updateUser(int userId, String username, String initial, String password, String email, Boolean admin, Boolean enabled) {
        updateUser(userId, username, initial, password, email, admin, enabled, null);
    }

    @Transactional
    public void updateUser(int userId, String username, String initial, String password, String email, Boolean admin,
                           Boolean enabled, String pmUserId) {
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
        if (pmUserId != null) requireIntegrationBindings().setUserBinding(user, pmUserId);
    }

    public UserEntity getCurrentUser(){
        return appUserDetailsService.getUserEntity();
    }

    public List<UserEntity> getAllUsers() {
        return userRepository.findAll();
    }

    public UserEntity updateCurrentUserEmail(String email) {
        UserEntity user = getCurrentUser();
        String normalizedEmail = normalizeEmail(email);

        if(normalizedEmail != null) {
            userRepository.findByEmail(normalizedEmail)
                    .filter(existingUser -> existingUser.getId() != user.getId())
                    .ifPresent(existingUser -> {
                        throw new ConflictException("Email already in use");
                    });
        }

        user.setEmail(normalizedEmail);
        return userRepository.save(user);
    }

    public void updatePassword(String oldPassword, String newPassword){
        UserEntity user = getCurrentUser();
        if(!PasswordEncoding.matches("bcrypt", oldPassword, user.getPassword())) {
            throw new UnauthorizedException("Password is not correct");
        }
        if(newPassword == null || newPassword.length() < 3) {
            throw new BadRequestException("Password must be at least 3 characters");
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
        String normalizedEmail = email.trim().toLowerCase();
        if(!EMAIL_PATTERN.matcher(normalizedEmail).matches()) {
            throw new BadRequestException("Email is not valid");
        }
        return normalizedEmail;
    }

    private IntegrationBindingService requireIntegrationBindings() {
        if (integrationBindingService == null) throw new IllegalStateException("PM integration service is unavailable");
        return integrationBindingService;
    }
}
