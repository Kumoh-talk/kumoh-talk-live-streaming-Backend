package com.kumoh_talk.streaming.global.auth.service;

import com.kumoh_talk.streaming.global.auth.constant.Role;
import com.kumoh_talk.streaming.global.auth.vo.AuthenticatedUser;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthenticatedUserService {

    public static final String USER_ID = "USER_ID";
    public static final String USER_NICKNAME = "USER_NICKNAME";
    public static final String USER_ROLE = "USER_ROLE";

    public UserDetails loadUserByClaims(Claims claims) {
        return AuthenticatedUser.builder()
                .userId(claims.get(USER_ID, Long.class))
                .nickname(claims.get(USER_NICKNAME, String.class))
                .role(Role.valueOf(claims.get(USER_ROLE, String.class)))
                .build();
    }
}