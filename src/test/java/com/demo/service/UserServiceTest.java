package com.demo.service;

import com.demo.dto.UserDto;
import com.demo.mapper.UserMapper;
import com.demo.model.its.User;
import com.demo.repository.its.UsersRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {
    @InjectMocks
    private UserService userService;

    @Mock
    private UsersRepository usersRepository;

    @Mock
    private UserMapper UserMapper;

    private User user1;
    private User user2;

    private UserDto dto1;
    private UserDto dto2;

    @BeforeEach
    void setup() {
        user1 = new User();
        user1.setUsername("Alice");

        user2 = new User();
        user2.setUsername("Bob");

        dto1 = new UserDto();
        dto1.setUsername("Alice");

        dto2 = new UserDto();
        dto2.setUsername("Bob");
    }

    @Test
    void testGetUserInfo() {
        when(usersRepository.findAll()).thenReturn(List.of(user1, user2));

        when(UserMapper.toDto(user1)).thenReturn(dto1);
        when(UserMapper.toDto(user2)).thenReturn(dto2);

        List<UserDto> result = userService.getUserInfo();

        assertEquals(2, result.size());
        assertEquals("Alice", result.get(0).getUsername());
        assertEquals("Bob", result.get(1).getUsername());
    }
}
