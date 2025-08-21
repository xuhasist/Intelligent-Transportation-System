package com.demo.model.its;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tc_info")
@Data   // includes getter、setter、toString、equals、hashCode
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class TcInfo {
    @Id
    @Column(name = "tc_id", length = 10, nullable = false)
    private String tcId;

    @Column(name = "name", length = 50)
    private String name;

    @Column(name = "group_number", nullable = false)
    private Integer groupNumber;

    @Column(name = "ip", length = 15, nullable = false)
    private String ip;

    @Column(name = "port", nullable = false)
    private Integer port;

    @Column(name = "addr", nullable = false)
    private Integer addr;

    @Column(name = "enable", nullable = false)
    private Byte enable;

    @Column(name = "dynamic_enable", nullable = false)
    private Byte dynamicEnable;

    @Column(name = "position_lon", nullable = false)
    private Double positionLon;

    @Column(name = "position_lat", nullable = false)
    private Double positionLat;
}
