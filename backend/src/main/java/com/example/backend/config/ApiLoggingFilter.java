package com.example.backend.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
public class ApiLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String uri = request.getRequestURI();

        // /actuator 헬스체크 및 프로메테우스 수집 요청은 로그 노이즈 방지를 위해 제외
        if (uri.startsWith("/actuator")) {
            filterChain.doFilter(request, response);
            return;
        }

        long startTime = System.currentTimeMillis();
        try {
            filterChain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            int status = response.getStatus();
            String queryString = request.getQueryString();
            String fullPath = (queryString != null && !queryString.isBlank())
                    ? uri + "?" + queryString
                    : uri;

            if (status >= 500) {
                log.error("🚨 [API] {} {} -> {} ({}ms)", request.getMethod(), fullPath, status, duration);
            } else if (status >= 400) {
                log.warn("⚠️ [API] {} {} -> {} ({}ms)", request.getMethod(), fullPath, status, duration);
            } else {
                log.info("✅ [API] {} {} -> {} ({}ms)", request.getMethod(), fullPath, status, duration);
            }
        }
    }
}
