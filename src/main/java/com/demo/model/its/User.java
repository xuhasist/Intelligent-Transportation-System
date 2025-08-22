package com.demo.model.its;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;

@Entity
@Table(name = "user")
@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class User {
    @Id
    @Column(name = "username", nullable = false, length = 100)
    private String username;

    @Column(name = "password", nullable = false, length = 255)
    private String password;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "user_role",
            joinColumns = @JoinColumn(name = "username"),       // tell Hibernate username from user_role references the primary key of users
            inverseJoinColumns = @JoinColumn(name = "role_id")) // tell Hibernate role_id from user_role references the primary key of roles
    @JsonIgnore // won't be shown on swagger UI
    private Set<Role> roles = new HashSet<>();
}
