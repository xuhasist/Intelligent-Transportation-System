package com.demo.service;

import com.demo.config.JwtConfig;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.crypto.SecretKey;

import java.lang.reflect.Field;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class JwtTokenServiceTest {

    @InjectMocks
    private JwtTokenService jwtTokenService;

    @Mock
    private JwtConfig jwtConfig;

    private SecretKey realKey;

    @BeforeEach
    void setUp() throws Exception {
        // create real secret key
        realKey = io.jsonwebtoken.security.Keys.secretKeyFor(SignatureAlgorithm.HS512);

        // access private field
        Field secretKeyField = JwtTokenService.class.getDeclaredField("secretKey");
        secretKeyField.setAccessible(true);
        secretKeyField.set(jwtTokenService, realKey);
    }

    @Test
    void testGenerateToken() throws NoSuchFieldException, IllegalAccessException {
        when(jwtConfig.getExpiration()).thenReturn(3600000L); // 1 hour in ms
        when(jwtConfig.getIssuer()).thenReturn("testIssuer");

        String token = jwtTokenService.generateToken("user1", "USER");

        assertNotNull(token);

        Claims claims = Jwts.parser()
                .verifyWith(realKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        assertEquals("user1", claims.getSubject());
        assertEquals("USER", claims.get("scope", String.class));
        assertEquals("testIssuer", claims.getIssuer());
        assertTrue(claims.getExpiration().after(new Date()));
    }

    @Test
    void testValidateToken() {
        when(jwtConfig.getExpiration()).thenReturn(3600000L); // 1 hour in ms
        String token = jwtTokenService.generateToken("user1", "USER");

        Claims claims = Jwts.parser()
                .verifyWith(realKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        // valid token, requirePasswordReset=false
        assertTrue(jwtTokenService.validateToken(claims, false));
        // valid token, requirePasswordReset=true
        assertTrue(jwtTokenService.validateToken(claims, true));

        // simulate PASSWORD_RESET token
        String resetToken = jwtTokenService.generateToken("user2", "PASSWORD_RESET");
        Claims resetClaims = Jwts.parser()
                .verifyWith(realKey)
                .build()
                .parseSignedClaims(resetToken)
                .getPayload();

        assertFalse(jwtTokenService.validateToken(resetClaims, false));
        assertTrue(jwtTokenService.validateToken(resetClaims, true));
    }

    @Test
    void testGetUsernameFromToken() {
        when(jwtConfig.getExpiration()).thenReturn(3600000L);
        String token = jwtTokenService.generateToken("user1", "USER");

        String username = jwtTokenService.getUsernameFromToken(token, false);
        assertEquals("user1", username);

        // invalid token
        String invalidUsername = jwtTokenService.getUsernameFromToken("fakeToken", false);
        assertEquals("invalid token", invalidUsername);
    }

    @Test
    void testNeedsAuthentication() {
        HttpServletRequest request = mock(HttpServletRequest.class);

        when(jwtConfig.getExpiration()).thenReturn(3600000L);
        String token = jwtTokenService.generateToken("user1", "USER");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);

        assertFalse(jwtTokenService.needsAuthentication(request));
        assertFalse(jwtTokenService.needsAuthentication(request, false));

        // invalid token
        when(request.getHeader("Authorization")).thenReturn("Bearer invalidToken");
        assertTrue(jwtTokenService.needsAuthentication(request));
    }
}
