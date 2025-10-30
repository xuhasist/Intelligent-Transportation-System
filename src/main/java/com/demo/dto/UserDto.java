package com.demo.dto;

import com.demo.model.its.Role;
import lombok.*;

import java.util.Set;

@Builder(toBuilder = true)
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class UserDto {
    private String username;
    private Set<Role> roles;
}
