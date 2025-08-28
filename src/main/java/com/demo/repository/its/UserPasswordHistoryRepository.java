package com.demo.repository.its;

import com.demo.model.its.UserPasswordHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserPasswordHistoryRepository extends JpaRepository<UserPasswordHistory, Long> {
    List<UserPasswordHistory> findTop3ByUsernameOrderByChangedAtDesc(String username);

    List<UserPasswordHistory> findByUsernameOrderByChangedAtDesc(String username);
}
