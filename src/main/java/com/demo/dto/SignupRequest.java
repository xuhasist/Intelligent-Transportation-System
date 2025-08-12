package com.demo.dto;

import lombok.*;

import java.util.*;

@Builder(toBuilder = true)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SignupRequest {
    private String username;
    private String password;
    private Set<String> roles;   // ["user", "admin"]
}
