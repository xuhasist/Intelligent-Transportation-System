package com.demo.service;

import com.demo.dto.ChangePasswordRequest;
import com.demo.enums.AuthDefine;
import com.demo.exception.CustomException;
import com.demo.model.its.Role;
import com.demo.model.its.User;
import com.demo.dto.JwtAuthenticationResponse;
import com.demo.dto.SignupRequest;
import com.demo.model.its.UserPasswordHistory;
import com.demo.repository.its.RoleRepository;
import com.demo.repository.its.UserPasswordHistoryRepository;
import com.demo.repository.its.UsersRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AuthService {
    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private UserPasswordHistoryRepository userPasswordHistoryRepository;

    @Autowired
    private RoleRepository roleRepository;

    private static final int MAX_PWD_ERROR_TIMES = 5;
    private static final int LOCK_PWD_MINUTES = 15;
    private static final int PWD_VALID_DAYS = 90;
    private static final String STRONG_PWD_REGEX =
            "^(?=.*[^a-zA-Z0-9])(?=.*[A-Z])(?=.*[a-z])(?=.*\\d).{8,}$";

    public JwtAuthenticationResponse authenticate(String username, String password) {
        User user = getUserOrThrow(username);

        int password_try_error_times = user.getPwdErrorTimes();
        LocalDateTime last_password_try_time = user.getPwdTryTime();
        if (!canTryPassword(password_try_error_times, last_password_try_time))
            throw new CustomException(AuthDefine.AccountLocked.getDescription() + " Remains " + remainingLockMinutes(last_password_try_time) + " minutes", HttpStatus.FORBIDDEN);

        if (!(passwordEncoder.matches(password, user.getPassword()))) {
            user.setPwdErrorTimes(user.getPwdErrorTimes() + 1);
            usersRepository.save(user);
            throw new CustomException(AuthDefine.AuthenticationFailed.getDescription(), HttpStatus.UNAUTHORIZED);
        } else {
            user.setPwdErrorTimes(0);
            usersRepository.save(user);
        }

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password)
        );

        // check roles
        Set<Role> roles = user.getRoles();
        boolean isAdmin = roles.stream()
                .anyMatch(role -> role.getName().equals("ADMIN"));
        boolean isUser = roles.stream()
                .anyMatch(role -> role.getName().equals("USER"));

        if (!isAdmin && !isUser) {
            throw new CustomException(AuthDefine.InvalidRole.getDescription(), HttpStatus.BAD_REQUEST);
        }

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwtClaimScope = "";
        String message = AuthDefine.AuthenticationSuccess.getDescription();
        int status = AuthDefine.AuthenticationSuccess.getStatusCode();

        if (isPasswordExpired(user.getChangedAt())) {
            jwtClaimScope = "PASSWORD_RESET";
            message = AuthDefine.PasswordExpired.getDescription();
            status = AuthDefine.PasswordExpired.getStatusCode();
        }

        String jwt = jwtTokenService.generateToken(authentication.getName(), jwtClaimScope);

        return new JwtAuthenticationResponse().toBuilder()
                .username(username)
                .accessToken(jwt)
                .message(message)
                .status(status)
                .build();
    }

    public URI register(SignupRequest signUpRequest) {
        if (usersRepository.existsByUsername(signUpRequest.getUsername()))
            throw new CustomException(AuthDefine.UsernameAlreadyInUse.getDescription(), HttpStatus.CONFLICT);

        String rawPassword = signUpRequest.getPassword();

        // Password strength validation
        if (isWeakPassword(rawPassword))
            throw new CustomException(AuthDefine.PasswordNotStrong.getDescription(), HttpStatus.BAD_REQUEST);

        Set<String> roles = signUpRequest.getRoles();
        Set<Role> roleSet = roles.stream()
                .map(role -> {
                    if (role.equalsIgnoreCase("ADMIN")) {
                        return roleRepository.findByname("ADMIN").orElseThrow(() -> new RuntimeException("Role not found: " + role));
                    } else if (role.equalsIgnoreCase("USER")) {
                        return roleRepository.findByname("USER").orElseThrow(() -> new RuntimeException("Role not found: " + role));
                    } else {
                        return roleRepository.findByname("USER").orElseThrow(() -> new RuntimeException("Role not found: " + role)); // default to USER
                    }
                }).collect(Collectors.toSet());

        String username = signUpRequest.getUsername();
        String password = passwordEncoder.encode(rawPassword);

        User user = User.builder()
                .username(signUpRequest.getUsername())
                .password(passwordEncoder.encode(signUpRequest.getPassword()))
                .roles(roleSet)
                .pwdErrorTimes(0)
                .build();
        this.usersRepository.save(user);

        updatePasswordHistory(username, password);

        return ServletUriComponentsBuilder.fromCurrentContextPath().path("/users/{username}")
                .buildAndExpand(user.getUsername()).toUri();
    }

    private User getUserOrThrow(String username) {
        return usersRepository.findByUsername(username)
                .orElseThrow(() -> new CustomException(
                        AuthDefine.UserNotExist.getDescription(),
                        HttpStatus.NOT_FOUND
                ));
    }

    public boolean canTryPassword(int passwordTryErrorTimes, LocalDateTime passwordTryTime) {
        if (passwordTryErrorTimes < MAX_PWD_ERROR_TIMES) {
            return true;
        }

        return remainingLockMinutes(passwordTryTime) <= 0;
    }

    public long remainingLockMinutes(LocalDateTime passwordTryTime) {
        LocalDateTime unlockTime = passwordTryTime.plusMinutes(LOCK_PWD_MINUTES);
        LocalDateTime now = LocalDateTime.now();

        long minutes = Duration.between(now, unlockTime).toMinutes();
        return Math.max(minutes, 0);    // No negative
    }

    public boolean isPasswordExpired(LocalDateTime changeAt) {
        if (changeAt == null) {
            return true;    // No record exists, treat as expired
        }

        LocalDateTime now = LocalDateTime.now();
        Duration duration = Duration.between(changeAt, now);

        return duration.toDays() >= PWD_VALID_DAYS;
    }

    public boolean isWeakPassword(String rawPassword) {
        // Password strength validation
        return !rawPassword.matches(STRONG_PWD_REGEX);
    }

    public void changePassword(ChangePasswordRequest request) {
        String username = request.getUsername();
        String oldPassword = request.getOldPassword();
        String newPassword = request.getNewPassword();

        User user = getUserOrThrow(username);

        if (!(passwordEncoder.matches(oldPassword, user.getPassword())))
            throw new CustomException(AuthDefine.AuthenticationFailed.getDescription(), HttpStatus.UNAUTHORIZED);

        if (this.isWeakPassword(newPassword)) {
            throw new CustomException(AuthDefine.PasswordNotStrong.getDescription(), HttpStatus.FORBIDDEN);
        }

        // Retrieve the latest 3 passwords
        List<UserPasswordHistory> lastPasswords = user.getPasswordHistories();

        // Check for duplicates
        String newPasswordHash = passwordEncoder.encode(newPassword);
        for (UserPasswordHistory ph : lastPasswords) {
            if (passwordEncoder.matches(newPassword, ph.getPasswordHash())) {
                throw new CustomException(AuthDefine.PasswordReused.getDescription(), HttpStatus.FORBIDDEN);
            }
        }

        // Update password and change timestamp
        user.setPassword(newPasswordHash);
        user.setChangedAt(LocalDateTime.now());
        usersRepository.save(user);

        updatePasswordHistory(username, newPasswordHash);
    }

    public void updatePasswordHistory(String username, String password) {
        UserPasswordHistory history = new UserPasswordHistory();
        history.setUser(getUserOrThrow(username));
        history.setPasswordHash(password);
        userPasswordHistoryRepository.save(history);

        // Delete old password records, keeping only the latest 3 entries
        List<UserPasswordHistory> all = userPasswordHistoryRepository.findByUserUsernameOrderByChangedAtDesc(username);
        if (all.size() > 3) {
            List<UserPasswordHistory> toDelete = all.subList(3, all.size());
            userPasswordHistoryRepository.deleteAll(toDelete);
        }
    }
}
