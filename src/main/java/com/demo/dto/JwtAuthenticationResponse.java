package com.demo.dto;

import lombok.*;

@Builder(toBuilder = true)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class JwtAuthenticationResponse {
    private String accessToken;
    //@Builder.Default
    private String tokenType = "Bearer";
    private String username;
    private Integer status = 0;
    private String message = "";
}
