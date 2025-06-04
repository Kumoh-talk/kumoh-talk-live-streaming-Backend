package com.kumoh_talk.streaming.global.socket.security;

import com.kumoh_talk.streaming.global.auth.jwt.TokenProvider;
import com.kumoh_talk.streaming.global.exception.ExceptionCode;
import com.kumoh_talk.streaming.global.exception.ServiceException;
import com.kumoh_talk.streaming.global.response.ResponseUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.Collection;

import static com.kumoh_talk.streaming.global.socket.constant.WebSocketConstants.ERROR_DESTINATION;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthValidator {

    public static final String AUTHORIZATION = "Authorization";

    private final SimpMessagingTemplate template;
    private final TokenProvider tokenProvider;
    private final RoleHierarchy roleHierarchy;

    public UserDetails validateTokenAndRole(StompHeaderAccessor accessor, String requiredRole) {
        String token = accessor.getFirstNativeHeader(AUTHORIZATION);
        UserDetails userDetails = tokenProvider.validateAndParseToken(token);

        if (userDetails == null) {
            this.sendErrorToUserAndThrow(accessor.getSessionId(), ExceptionCode.UN_AUTHENTICATION);
        }

        if (!isValidRole(userDetails, requiredRole) || isUserBlocked(userDetails)) {
            this.sendErrorToUserAndThrow(accessor.getUser().getName(), ExceptionCode.FORBIDDEN);
        }

        return userDetails;
    }

    private boolean isValidRole(UserDetails user, String requiredRole) {
        Collection<? extends GrantedAuthority> reachableAuthorities =
                roleHierarchy.getReachableGrantedAuthorities(user.getAuthorities());

        return reachableAuthorities.stream()
                .anyMatch(a -> a.getAuthority().equals(requiredRole));
    }

    public boolean isUserBlocked(UserDetails userDetails) {
        // TODO. 차단 조회
        return false;
    }

    private void sendErrorToUserAndThrow(String sendTo, ExceptionCode code) {
        template.convertAndSendToUser(sendTo, ERROR_DESTINATION, ResponseUtil.createFailureResponse(code));
        throw ServiceException.from(code);
    }
}
