package com.demo.repository.dynamic;

import com.demo.model.dynamic.DynamicParameters;
import com.demo.model.dynamic.DynamicParametersId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DynamicParametersRepository extends JpaRepository<DynamicParameters, DynamicParametersId> {
    List<DynamicParameters> findByIdProgramIdAndIdDeviceIdAndIdPlanId(
            String programId, String deviceId, int planId
    );
}
