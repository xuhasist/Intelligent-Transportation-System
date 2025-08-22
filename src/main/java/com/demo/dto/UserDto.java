package com.demo.dto;

import com.demo.model.its.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Builder(toBuilder = true)
@Setter
@Getter
@AllArgsConstructor
public class UserDto {
    private String username;
    private Set<Role> roles;
}
