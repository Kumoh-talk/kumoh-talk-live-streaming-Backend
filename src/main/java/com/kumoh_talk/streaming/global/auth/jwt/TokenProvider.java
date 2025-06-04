package com.kumoh_talk.streaming.global.auth.jwt;

import com.kumoh_talk.streaming.global.auth.service.AuthenticatedUserService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Slf4j
@RequiredArgsConstructor
@Component
public class TokenProvider {

    private static final String BEARER_PREFIX = "Bearer ";

    private final AuthenticatedUserService authenticatedUserService;

    @Value("${jwt.secret-key}")
    private String secretKey;

    public UserDetails validateAndParseToken(String bearerToken) {
        String token = this.resolveToken(bearerToken);

        if (token == null) {
            return null;
        }

        try {
            Claims claims = Jwts.parser()
                    .verifyWith(Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8)))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return authenticatedUserService.loadUserByClaims(claims);

        } catch (ExpiredJwtException e) {
            log.error("JWT가 만료되었습니다.");

        } catch (Exception e) {
            log.error("토큰이 유효하지 않습니다.");
        }

        return null;
    }

    private String resolveToken(String bearerToken) {
        if (bearerToken != null && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}