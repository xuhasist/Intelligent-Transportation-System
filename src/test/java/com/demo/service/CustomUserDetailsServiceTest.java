package com.demo.service;

import com.demo.model.its.Role;
import com.demo.model.its.User;
import com.demo.repository.its.UsersRepository;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;
import java.util.Set;

@ExtendWith(MockitoExtension.class)
public class CustomUserDetailsServiceTest {
    @Mock
    private UsersRepository usersRepository;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void loadUserByUsername_UserExists_ReturnsUserDetails() {
        String username = "testUser";
        String password = "password123";

        User mockUser = new User();
        mockUser.setUsername(username);
        mockUser.setPassword(password);
        mockUser.setRoles(Set.of(Role.builder().name("USER").build()));

        when(usersRepository.findByUsername(username)).thenReturn(Optional.of(mockUser));

        UserDetails userDetails = customUserDetailsService.loadUserByUsername(username);

        assertNotNull(userDetails);
        assertEquals(username, userDetails.getUsername());
        assertEquals(password, userDetails.getPassword());
        assertTrue(userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("USER")));

        verify(usersRepository, times(1)).findByUsername(username);
    }

    @Test
    void loadUserByUsername_UserDoesNotExist_ThrowsException() {
        String username = "nonExistentUser";
        when(usersRepository.findByUsername(username)).thenReturn(Optional.empty());

        UsernameNotFoundException exception = assertThrows(
                UsernameNotFoundException.class,
                () -> customUserDetailsService.loadUserByUsername(username)
        );

        assertEquals("User not found with username: " + username, exception.getMessage());
        verify(usersRepository, times(1)).findByUsername(username);
    }
}
