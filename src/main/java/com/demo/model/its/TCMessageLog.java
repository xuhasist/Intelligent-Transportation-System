package com.demo.model.its;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;


@Entity
@Table(name = "tc_message_log")
@Data   // includes getter、setter、toString、equals、hashCode
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class TCMessageLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_id", nullable = false, length = 10)
    private String deviceId;

    @Column(name = "message_id", nullable = false, length = 10)
    private String messageId;

    @Column(name = "json_value", nullable = false, columnDefinition = "TEXT")
    private String jsonValue;

    @Column(name = "raw_value", length = 255)
    private String rawValue;

    @Column(name = "note_code", nullable = false)
    private Integer noteCode;

    @Column(name = "return_result", length = 255)
    private String returnResult;

    @Column(name = "log_time", nullable = false, insertable = false, updatable = false,
            columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime logTime;
}


