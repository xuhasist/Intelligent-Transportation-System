package com.demo.service;

import com.demo.config.JwtConfig;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.*;

@Service
public class JwtTokenService {

    @Autowired
    private JwtConfig jwtConfig;

    @Autowired
    private SecretKey secretKey;

    // create JWT
    public String generateToken(String username) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtConfig.getExpiration());

        return Jwts.builder()
                .subject(username)
                .issuedAt(now)
                .expiration(expiryDate)
                .issuer(jwtConfig.getIssuer())
                .signWith(secretKey, Jwts.SIG.HS512)
                .compact();
    }

    public String getUsernameFromToken(String token) {
        return parseToken(token)
                .map(Claims::getSubject)
                .orElse("invalid token");
    }

    public boolean needsAuthentication(HttpServletRequest request) {
        return getTokenFromRequest(request)
                .map(this::getUsernameFromToken)
                .map(username -> username.equals("invalid token"))
                .orElse(true);
    }

    private Optional<Claims> parseToken(String token) {
        try {
            return Optional.of(
                    Jwts.parser()
                            .verifyWith(secretKey)         // must be SecretKey type
                            .build()                       // create parser
                            .parseSignedClaims(token)      // validate and parse JWT, if invalid, will throw exception
                            .getPayload()                  // get claims
            );
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    // get Bearer token from request header
    private Optional<String> getTokenFromRequest(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return Optional.of(header.substring(7));    // "Bearer ".length()
        }
        return Optional.empty();
    }
}
