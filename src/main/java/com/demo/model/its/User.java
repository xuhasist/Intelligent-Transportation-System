package com.demo.model.its;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "user")
@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class User {
    @Id
    @NotBlank(message = "Username is required")
    @Column(name = "username", nullable = false, length = 100)
    private String username;

    @NotBlank(message = "Password is required")
    @Column(name = "password", nullable = false, length = 255)
    private String password;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "user_role",
            joinColumns = @JoinColumn(name = "username"),       // tell Hibernate username from user_role references the primary key of users
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    // tell Hibernate role_id from user_role references the primary key of roles
    @JsonIgnore // won't be shown on swagger UI
    private Set<Role> roles = new HashSet<>();

    // map to UserPasswordHistory.user
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JsonIgnore
    private List<UserPasswordHistory> passwordHistories;

    @JsonIgnore
    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @JsonIgnore
    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    @JsonIgnore
    @Column(name = "pwd_error_times", nullable = false)
    private int pwdErrorTimes;

    @JsonIgnore
    @Column(name = "pwd_try_time", nullable = false, insertable = false, updatable = false)
    private LocalDateTime pwdTryTime;

    @JsonIgnore
    @Column(name = "changed_at", nullable = false, insertable = false)
    private LocalDateTime changedAt;
}
