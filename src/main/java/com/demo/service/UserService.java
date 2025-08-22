package com.demo.service;

import com.demo.dto.UserDto;
import com.demo.mapper.UserMapper;
import com.demo.model.its.User;
import com.demo.repository.its.UsersRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {
    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private UserMapper UserMapper;

    @Cacheable("userInfo")
    public List<UserDto> getUserInfo() {
        List<User> users = usersRepository.findAll();
        return users.stream().map(UserMapper::toDto).toList();
    }
}
