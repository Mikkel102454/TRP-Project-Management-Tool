package solutions.trp.pmt.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import solutions.trp.pmt.controller.api.execption.BadRequestException;
import solutions.trp.pmt.controller.api.execption.ConflictException;
import solutions.trp.pmt.controller.api.execption.UnauthorizedException;
import solutions.trp.pmt.datasource.users.UserEntity;
import solutions.trp.pmt.datasource.users.UserRepository;
import solutions.trp.pmt.util.PasswordEncoding;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServiceTest {
    private UserRepository userRepository;
    private AppUserDetailsService appUserDetailsService;
    private UserService userService;
    private UserEntity currentUser;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        appUserDetailsService = mock(AppUserDetailsService.class);
        userService = new UserService(userRepository, appUserDetailsService);
        currentUser = new UserEntity();

        when(appUserDetailsService.getUserEntity()).thenReturn(currentUser);
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void updateCurrentUserEmailNormalizesAndSavesEmail() {
        userService.updateCurrentUserEmail("  Person@Example.COM ");

        assertEquals("person@example.com", currentUser.getEmail());
        verify(userRepository).findByEmail("person@example.com");
        verify(userRepository).save(currentUser);
    }

    @Test
    void updateCurrentUserEmailClearsEmail() {
        currentUser.setEmail("person@example.com");

        userService.updateCurrentUserEmail("   ");

        assertNull(currentUser.getEmail());
        verify(userRepository, never()).findByEmail(any());
        verify(userRepository).save(currentUser);
    }

    @Test
    void updateCurrentUserEmailAllowsCurrentEmail() {
        currentUser.setEmail("person@example.com");
        when(userRepository.findByEmail("person@example.com")).thenReturn(Optional.of(currentUser));

        userService.updateCurrentUserEmail("person@example.com");

        assertEquals("person@example.com", currentUser.getEmail());
        verify(userRepository).save(currentUser);
    }

    @Test
    void updateCurrentUserEmailRejectsMalformedEmail() {
        assertThrows(
                BadRequestException.class,
                () -> userService.updateCurrentUserEmail("not-an-email")
        );

        verify(userRepository, never()).save(any());
    }

    @Test
    void updateCurrentUserEmailRejectsAnotherUsersEmail() {
        UserEntity anotherUser = mock(UserEntity.class);
        when(anotherUser.getId()).thenReturn(2);
        when(userRepository.findByEmail("used@example.com")).thenReturn(Optional.of(anotherUser));

        assertThrows(
                ConflictException.class,
                () -> userService.updateCurrentUserEmail("used@example.com")
        );

        verify(userRepository, never()).save(any());
    }

    @Test
    void updatePasswordChangesPassword() {
        currentUser.setPassword(PasswordEncoding.encode("bcrypt", "current"));

        userService.updatePassword("current", "new-password");

        assertTrue(PasswordEncoding.matches("bcrypt", "new-password", currentUser.getPassword()));
        verify(userRepository).save(currentUser);
    }

    @Test
    void updatePasswordRejectsIncorrectCurrentPassword() {
        currentUser.setPassword(PasswordEncoding.encode("bcrypt", "current"));

        assertThrows(
                UnauthorizedException.class,
                () -> userService.updatePassword("incorrect", "new-password")
        );

        verify(userRepository, never()).save(any());
    }

    @Test
    void updatePasswordRejectsTooShortNewPassword() {
        currentUser.setPassword(PasswordEncoding.encode("bcrypt", "current"));

        assertThrows(
                BadRequestException.class,
                () -> userService.updatePassword("current", "ab")
        );

        verify(userRepository, never()).save(any());
    }
}
