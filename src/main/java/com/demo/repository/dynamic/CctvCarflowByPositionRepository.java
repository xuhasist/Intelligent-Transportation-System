package com.demo.repository.dynamic;

import com.demo.model.dynamic.CctvCarflowInstant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface CctvCarflowByPositionRepository extends JpaRepository<CctvCarflowInstant, Long> {
    @Query(value = "SELECT COALESCE(SUM(motor) + SUM(car) + SUM(truck),0) FROM cctv_carflow_instant WHERE cctv_id = :cctv_id and start_time >= (:end_time - INTERVAL :minute MINUTE) AND end_time < :end_time", nativeQuery = true)
    double findCarflowSumByCctvIdAndEndTime(
            @Param("cctv_id") String cctv_id,
            @Param("end_time") LocalDateTime end_time,
            @Param("minute") int minute
    );

    @Query(value = "SELECT COALESCE(SUM(motor) + SUM(car) + SUM(truck),0) FROM cctv_carflow_instant WHERE cctv_id = :cctv_id and start_time >= (:end_time - INTERVAL :minute MINUTE) AND end_time < :end_time and start_position = :startPos and end_position = :endPos", nativeQuery = true)
    double findCarflowSumByCctvIdAndEndTimeAndStartPositionAndEndPosition(
            @Param("cctv_id") String cctv_id,
            @Param("end_time") LocalDateTime end_time,
            @Param("minute") int minute,
            @Param("startPos") String startPos,
            @Param("endPos") String endPos
    );
}
