package com.demo.model.dynamic;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data   // includes getter、setter、toString、equals、hashCode
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Embeddable
public class DynamicThresholdId implements Serializable {

    @Column(name = "program_id", nullable = false)
    private String programId;

    @Column(name = "sub_id", nullable = false)
    private Integer subId;
}
