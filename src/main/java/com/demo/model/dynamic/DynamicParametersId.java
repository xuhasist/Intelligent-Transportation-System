package com.demo.model.dynamic;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Data   // includes getter、setter、toString、equals、hashCode
@Builder
@Embeddable
public class DynamicParametersId implements Serializable {

    @Column(name = "program_id", length = 10, nullable = false)
    private String programId;

    @Column(name = "device_id", length = 10, nullable = false)
    private String deviceId;

    @Column(name = "plan_id", nullable = false)
    private Integer planId;

    @Column(name = "subphase_id", nullable = false)
    private Integer subphaseId;
}
