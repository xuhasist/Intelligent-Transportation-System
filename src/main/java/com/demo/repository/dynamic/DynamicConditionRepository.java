package com.demo.repository.dynamic;

import com.demo.model.dynamic.DynamicCondition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DynamicConditionRepository extends JpaRepository<DynamicCondition, String> {
}
