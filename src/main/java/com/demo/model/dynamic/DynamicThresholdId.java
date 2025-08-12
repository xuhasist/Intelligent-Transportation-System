package com.demo.model.dynamic;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Data;

import java.io.Serializable;

@Data   // includes getter、setter、toString、equals、hashCode
@Embeddable
public class DynamicThresholdId implements Serializable {

    @Column(name = "program_id", nullable = false)
    private String programId;

    @Column(name = "sub_id", nullable = false)
    private Integer subId;
}
