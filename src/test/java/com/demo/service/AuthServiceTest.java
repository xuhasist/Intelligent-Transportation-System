package com.demo.service;

import com.demo.dto.ChangePasswordRequest;
import com.demo.dto.JwtAuthenticationResponse;
import com.demo.dto.SignupRequest;
import com.demo.enums.AuthDefine;
import com.demo.exception.CustomException;
import com.demo.model.its.Role;
import com.demo.model.its.User;
import com.demo.model.its.UserPasswordHistory;
import com.demo.repository.its.RoleRepository;
import com.demo.repository.its.UserPasswordHistoryRepository;
import com.demo.repository.its.UsersRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {
    @InjectMocks
    private AuthService authService;

    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtTokenService jwtTokenService;
    @Mock
    private UsersRepository usersRepository;
    @Mock
    private UserPasswordHistoryRepository userPasswordHistoryRepository;
    @Mock
    private RoleRepository roleRepository;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .username("testUser")
                .password("encodedPassword")
                .pwdErrorTimes(0)
                .roles(Set.of(Role.builder().name("USER").build()))
                .changedAt(LocalDateTime.now().minusDays(10))
                .build();
    }

    @Test
    void authenticate_success() {
        when(usersRepository.findByUsername("testUser")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("1234", "encodedPassword")).thenReturn(true);
        Authentication auth = mock(Authentication.class);
        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(auth.getName()).thenReturn("testUser");
        when(jwtTokenService.generateToken(eq("testUser"), anyString())).thenReturn("jwt-token");

        JwtAuthenticationResponse response = authService.authenticate("testUser", "1234");

        assertEquals("testUser", response.getUsername());
        assertEquals("jwt-token", response.getAccessToken());
        assertEquals(AuthDefine.AuthenticationSuccess.getStatusCode(), response.getStatus());
    }

    @Test
    void authenticate_wrongPassword_shouldThrow() {
        when(usersRepository.findByUsername("testUser")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "encodedPassword")).thenReturn(false);

        // throw CustomException
        CustomException exception = assertThrows(CustomException.class, () -> authService.authenticate("testUser", "wrong"));
        assertEquals(AuthDefine.AuthenticationFailed.getDescription(), exception.getMessage());
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
    }

    @Test
    void authenticate_accountLocked_shouldThrow() {
        user.setPwdErrorTimes(5);
        user.setPwdTryTime(LocalDateTime.now());
        when(usersRepository.findByUsername("testUser")).thenReturn(Optional.of(user));

        CustomException exception = assertThrows(CustomException.class, () -> authService.authenticate("testUser", "1234"));
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
    }

    @Test
    void authenticate_passwordExpired_shouldSetResetScope() {
        user.setChangedAt(LocalDateTime.now().minusDays(90)); // 90days password expired
        when(usersRepository.findByUsername("testUser")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("1234", "encodedPassword")).thenReturn(true);

        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("testUser");
        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(jwtTokenService.generateToken(eq("testUser"), anyString())).thenReturn("jwt-token");

        JwtAuthenticationResponse response = authService.authenticate("testUser", "1234");

        assertEquals("testUser", response.getUsername());
        assertEquals("jwt-token", response.getAccessToken());
        assertEquals(AuthDefine.PasswordExpired.getStatusCode(), response.getStatus());

        ArgumentCaptor<String> usernameCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> scopeCaptor = ArgumentCaptor.forClass(String.class);

        // verify generateToken is called and capture parameters
        verify(jwtTokenService, times(1)).generateToken(usernameCaptor.capture(), scopeCaptor.capture());
        assertEquals("PASSWORD_RESET", scopeCaptor.getValue());
    }

    @Test
    void register_success() {
        SignupRequest request = new SignupRequest();
        request.setUsername("newUser");
        request.setPassword("strongPassword123!");
        request.setRoles(Set.of("USER"));

        User newuser = new User();
        newuser.setUsername(request.getUsername());
        newuser.setPassword("encodedPassword");

        when(usersRepository.findByUsername("newUser")).thenReturn(Optional.of(newuser));
        when(usersRepository.existsByUsername("newUser")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(roleRepository.findByname("USER")).thenReturn(Optional.of(Role.builder().name("USER").build()));

        // fake http request context
        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        // set mockRequest server name and port
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(mockRequest));

        URI uri = authService.register(request);

        // location: http://host:port/users/{username}
        assertTrue(uri.toString().contains("/users/"));

        verify(usersRepository, times(1)).save(any(User.class));
        verify(passwordEncoder, times(2)).encode(anyString());
        verify(roleRepository, times(1)).findByname("USER");

        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void register_weakPassword_shouldThrow() {
        SignupRequest request = new SignupRequest();
        request.setUsername("newUser");
        request.setPassword("weak");
        request.setRoles(Set.of("USER"));

        when(usersRepository.existsByUsername("newUser")).thenReturn(false);

        assertThrows(CustomException.class, () -> authService.register(request));
    }

    @Test
    void register_duplicateUsername_shouldThrow() {
        SignupRequest request = new SignupRequest();
        request.setUsername("dupUser");
        request.setPassword("strongPassword123");
        request.setRoles(Set.of("USER"));

        when(usersRepository.existsByUsername("dupUser")).thenReturn(true);

        assertThrows(CustomException.class, () -> authService.register(request));
    }

    @Test
    void changePassword_success() {
        UserPasswordHistory history = new UserPasswordHistory();
        history.setPasswordHash(user.getPassword());
        user.setPasswordHistories(List.of(history));

        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setUsername("testuser");
        req.setOldPassword("oldStrongPassword123!");
        req.setNewPassword("newStrongPassword123!");

        when(usersRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("oldStrongPassword123!", "encodedPassword")).thenReturn(true);
        when(passwordEncoder.encode("newStrongPassword123!")).thenReturn("newEncodedPassword");

        authService.changePassword(req);

        verify(usersRepository, times(1)).save(any(User.class));
        verify(userPasswordHistoryRepository, atLeastOnce()).save(any(UserPasswordHistory.class));
    }

    @Test
    void changePassword_weak_shouldThrow() {
        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setUsername("testuser");
        req.setOldPassword("old");
        req.setNewPassword("weak");

        when(usersRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("old", "encodedPassword")).thenReturn(true);

        assertThrows(CustomException.class, () -> authService.changePassword(req));
    }

    @Test
    void test_isWeakPassword() {
        assertTrue(authService.isWeakPassword("abc"));  // weak
        assertFalse(authService.isWeakPassword("Strong@123")); // strong enough
    }

    @Test
    void test_isPasswordExpired() {
        assertTrue(authService.isPasswordExpired(LocalDateTime.now().minusDays(90)));
        assertFalse(authService.isPasswordExpired(LocalDateTime.now().minusDays(10)));
    }

    @Test
    void test_canTryPassword() {
        assertTrue(authService.canTryPassword(3, LocalDateTime.now()));
        assertFalse(authService.canTryPassword(5, LocalDateTime.now()));
    }

    @Test
    void authenticate_userNotExist_shouldThrow() {
        when(usersRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        CustomException exception = assertThrows(CustomException.class, () -> authService.authenticate("nonexistent", "1234"));
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        assertEquals(AuthDefine.UserNotExist.getDescription(), exception.getMessage());
    }

    @Test
    void authenticate_noValidRole_shouldThrow() {
        user.setRoles(Set.of()); // empty roles
        when(usersRepository.findByUsername("testUser")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("1234", "encodedPassword")).thenReturn(true);
        when(authenticationManager.authenticate(any())).thenReturn(mock(Authentication.class));

        CustomException exception = assertThrows(CustomException.class, () -> authService.authenticate("testUser", "1234"));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertEquals(AuthDefine.InvalidRole.getDescription(), exception.getMessage());
    }

    @Test
    void canTryPassword_afterLockTimePassed_shouldReturnTrue() {
        LocalDateTime past = LocalDateTime.now().minusMinutes(15 + 1);
        assertTrue(authService.canTryPassword(5, past));
    }

    @Test
    void updatePasswordHistory_shouldDeleteOldPasswords() {
        User userWithHistory = User.builder().username("testuser").build();
        List<UserPasswordHistory> histories = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            UserPasswordHistory h = new UserPasswordHistory();
            h.setUser(userWithHistory);
            h.setPasswordHash("pwd" + i);
            histories.add(h);
        }

        when(usersRepository.findByUsername("testuser")).thenReturn(Optional.of(userWithHistory));
        when(userPasswordHistoryRepository.findByUserUsernameOrderByChangedAtDesc("testuser")).thenReturn(histories);

        authService.updatePasswordHistory("testuser", "newPwd");

        // verify deleteAll called for old passwords (index 3,4)
        verify(userPasswordHistoryRepository).deleteAll(histories.subList(3, 5));
    }

    @Test
    void register_roleNotFound_shouldThrowRuntimeException() {
        SignupRequest request = new SignupRequest();
        request.setUsername("newUser");
        request.setPassword("Strong@123");
        request.setRoles(Set.of("USER"));

        when(usersRepository.existsByUsername("newUser")).thenReturn(false);
        when(roleRepository.findByname("USER")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> authService.register(request));
    }

    @Test
    void changePassword_reusedPassword_shouldThrow() {
        UserPasswordHistory history = new UserPasswordHistory();
        history.setPasswordHash("encodedNewPassword");
        user.setPasswordHistories(List.of(history));

        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setUsername("testuser");
        req.setOldPassword("oldPassword");
        req.setNewPassword("newPassword123!");

        when(usersRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("oldPassword", "encodedPassword")).thenReturn(true);
        when(passwordEncoder.matches("newPassword123!", "encodedNewPassword")).thenReturn(true);

        CustomException exception = assertThrows(CustomException.class, () -> authService.changePassword(req));
        assertEquals(AuthDefine.PasswordReused.getDescription(), exception.getMessage());
    }
}
