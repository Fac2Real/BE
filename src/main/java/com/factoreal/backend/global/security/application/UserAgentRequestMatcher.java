package com.factoreal.backend.global.security.application;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.web.util.matcher.RequestMatcher;

public class UserAgentRequestMatcher implements RequestMatcher {
    private final String allowedUserAgent;
    public UserAgentRequestMatcher(String allowedUserAgent) {
        this.allowedUserAgent = allowedUserAgent;
    }
    @Override
    public boolean matches(HttpServletRequest request) {
        String userAgent = request.getHeader("Referer");
        return userAgent != null && userAgent.contains(this.allowedUserAgent);
    }
}
