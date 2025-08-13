package com.demo.repository.dynamic;

import com.demo.model.dynamic.DynamicLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DynamicLogRepository extends JpaRepository<DynamicLog, Long> {
}
