package com.demo.repository.dynamic;

import com.demo.model.dynamic.DynamicPlanid;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DynamicPlanidRepository extends JpaRepository<DynamicPlanid, String> {
    List<DynamicPlanid> findByProgramIdAndDay(String programId, String day);
}