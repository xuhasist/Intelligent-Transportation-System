package com.demo.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AuthDefine {
    AuthenticationSuccess(0, "Authentication success"),
    AuthenticationFailed(1, "Incorrect username or password, or session expired. Please log in again."),
    UsernameAlreadyInUse(2, "This username is already taken."),
    PasswordNotStrong(3, "Password must be at least 8 characters long and include uppercase letters, lowercase letters, numbers, and special symbols."),
    InvalidToken(4, "Invalid or expired token."),
    InvalidRole(5, "Invalid role."),
    //DataNotFound(6, "Data not found."),
    UserNotExist(7, "The user does not exist."),
    PasswordReused(8, "Password cannot be the same as any of the previous 3 passwords."),
    PasswordExpired(9, "Password has expired after 90 days."),
    AccountLocked(10, "Account is locked after exceeding 5 failed password attempts.");

    private final int statusCode;
    private final String description;
}
