package com.demo.repository.its;

import com.demo.model.its.Users;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsersRepository extends JpaRepository<Users, String> {
    Users findByUsername(String username);

    Boolean existsByUsername(String username);
}
