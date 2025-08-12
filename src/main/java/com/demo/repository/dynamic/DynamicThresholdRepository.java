package com.demo.repository.dynamic;

import com.demo.model.dynamic.DynamicThreshold;
import com.demo.model.dynamic.DynamicThresholdId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DynamicThresholdRepository extends JpaRepository<DynamicThreshold, DynamicThresholdId> {
}
