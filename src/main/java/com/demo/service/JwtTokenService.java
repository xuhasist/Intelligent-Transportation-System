package com.demo.service;

import com.demo.config.JwtConfig;
import io.jsonwebtoken.*;
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
    public String generateToken(String username, String scope) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtConfig.getExpiration());

        return Jwts.builder()
                .subject(username)
                .claim("scope", scope)
                .issuedAt(now)
                .expiration(expiryDate)
                .issuer(jwtConfig.getIssuer())
                .signWith(secretKey, Jwts.SIG.HS512)
                .compact();
    }

    public boolean validateToken(Claims claims, boolean requirePasswordReset) {
        try {
            // expiration check
            if (claims.getExpiration().before(new Date())) {
                return false;
            }

            String scope = claims.get("scope", String.class);
            if (!requirePasswordReset) {
                return !"PASSWORD_RESET".equals(scope);
            }

            return true;

        } catch (JwtException | IllegalArgumentException e) {
            // Includes signature errors, format errors, expiration, etc.
            return false;
        }
    }

    public String getUsernameFromToken(String token, boolean requirePasswordReset) {
        return parseToken(token)
                .filter(claims -> validateToken(claims, requirePasswordReset))
                .map(Claims::getSubject)
                .orElse("invalid token");
    }

    // default requirePasswordReset = false
    public boolean needsAuthentication(HttpServletRequest request) {
        return needsAuthentication(request, false);
    }

    public boolean needsAuthentication(HttpServletRequest request, boolean requirePasswordReset) {
        return getTokenFromRequest(request)
                .map(token -> getUsernameFromToken(token, requirePasswordReset))
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
