package com.demo.repository.its;

import com.demo.model.its.TcInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TcInfoRepository extends JpaRepository<TcInfo, String> {
    TcInfo findByIp(String ip);

    TcInfo findByTcId(String tc_id);

    List<TcInfo> findByEnable(Byte enable);
}
