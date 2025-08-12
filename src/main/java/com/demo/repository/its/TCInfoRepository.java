package com.demo.repository.its;

import com.demo.model.its.TCInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TCInfoRepository extends JpaRepository<TCInfo, String> {
    TCInfo findByIp(String ip);

    TCInfo findByTcId(String tc_id);

    List<TCInfo> findByEnable(Byte enable);
}
