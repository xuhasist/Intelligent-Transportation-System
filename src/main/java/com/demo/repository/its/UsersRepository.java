package com.demo.repository.its;

import com.demo.model.its.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsersRepository extends JpaRepository<User, String> {
    User findByUsername(String username);

    Boolean existsByUsername(String username);
}
