package com.kumoh_talk.streaming.global.auth.vo;

import com.kumoh_talk.streaming.global.auth.constant.Role;
import lombok.Builder;

@Builder
public record AuthenticatedUser(
    Long userId,
    String nickname,
    Role role
) {
}
