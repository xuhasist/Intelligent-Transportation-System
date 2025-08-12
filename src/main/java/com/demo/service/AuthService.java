package com.demo.service;

import com.demo.enums.ErrorDefine;
import com.demo.exception.CustomException;
import com.demo.model.its.Role;
import com.demo.model.its.Users;
import com.demo.dto.JwtAuthenticationResponse;
import com.demo.dto.SignupRequest;
import com.demo.repository.its.RoleRepository;
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
    private RoleRepository roleRepository;

    public JwtAuthenticationResponse authenticate(String username, String password) {
        Users findUser = usersRepository.findByUsername(username);

        if ((findUser == null) || !(passwordEncoder.matches(password, findUser.getPassword()))) {
            throw new CustomException(ErrorDefine.AuthenticationFailed.getDescription(), HttpStatus.UNAUTHORIZED);
        }

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password)
        );

        // check roles
        Set<Role> roles = findUser.getRoles();
        boolean isAdmin = roles.stream()
                .anyMatch(role -> role.getName().equals("ADMIN"));
        boolean isUser = roles.stream()
                .anyMatch(role -> role.getName().equals("USER"));

        if (!isAdmin && !isUser) {
            throw new CustomException(ErrorDefine.InvalidRole.getDescription(), HttpStatus.BAD_REQUEST);
        }

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtTokenService.generateToken(authentication.getName());

        return new JwtAuthenticationResponse().toBuilder()
                .username(username)
                .accessToken(jwt)
                .build();
    }

    public URI register(SignupRequest signUpRequest) {
        if (usersRepository.existsByUsername(signUpRequest.getUsername()))
            throw new CustomException(ErrorDefine.UsernameAlreadyInUse.getDescription(), HttpStatus.CONFLICT);

        String rawPassword = signUpRequest.getPassword();

        // Password strength validation
        if (!rawPassword.matches("^(?=.*[^a-zA-Z0-9])(?=.*[A-Z])(?=.*[a-z])(?=.*\\d).{6,}$")) {
            throw new CustomException(ErrorDefine.PasswordSettingFailed.getDescription(), HttpStatus.BAD_REQUEST);
        }

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

        Users user = Users.builder()
                .username(signUpRequest.getUsername())
                .password(passwordEncoder.encode(signUpRequest.getPassword()))
                .roles(roleSet)
                .build();
        this.usersRepository.save(user);

        return ServletUriComponentsBuilder.fromCurrentContextPath().path("/users/{username}")
                .buildAndExpand(user.getUsername()).toUri();
    }
}
