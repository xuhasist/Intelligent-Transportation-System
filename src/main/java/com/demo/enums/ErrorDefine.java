package com.demo.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorDefine {
    AuthenticationFailed("Incorrect username or password, or session expired. Please log in again."),
    UsernameAlreadyInUse("This username is already taken."),
    PasswordSettingFailed("Password must be at least 8 characters long and include uppercase letters, lowercase letters, numbers, and special symbols."),
    InvalidToken("Invalid or expired token."),
    InvalidRole("Invalid role."),
    DataNotFound("Data not found.");

    private final String description;
}
