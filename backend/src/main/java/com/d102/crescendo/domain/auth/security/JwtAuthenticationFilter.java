package com.d102.crescendo.domain.auth.security;

import com.d102.crescendo.global.response.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.util.AntPathMatcher;

import java.io.IOException;

/**
 * JWT 인증 필터
 * - /api/auth/**, Swagger, OPTIONS 요청은 필터를 건너뜁니다.
 * - Authorization 헤더가 없으면 그냥 다음 필터로 넘깁니다.
 * - 토큰이 있을 경우만 검증 및 인증 객체 등록을 수행합니다.
 */
@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService customUserDetailsService;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final AntPathMatcher matcher = new AntPathMatcher();

    public JwtAuthenticationFilter(
            JwtTokenProvider jwtTokenProvider,
            CustomUserDetailsService customUserDetailsService,
            RedisTemplate<String, String> redisTemplate
    ) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.customUserDetailsService = customUserDetailsService;
        this.redisTemplate = redisTemplate;
    }

    /**
     * 필터를 적용하지 않을 경로 설정
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();

        log.info("[JWT FILTER] path={}, method={}", path, method);

        // 1️⃣ CORS preflight(OPTIONS) 요청은 항상 통과
        if ("OPTIONS".equalsIgnoreCase(method)) {
            log.info("[JWT FILTER] CORS preflight - 필터 건너뜀");
            return true;
        }

        // 2️⃣ 인증 불필요한 경로들
        boolean shouldSkip = matcher.match("/api/auth/**", path) ||
                matcher.match("/v3/api-docs/**", path) ||
                matcher.match("/api-docs/**", path) ||
                matcher.match("/swagger-ui/**", path) ||
                matcher.match("/swagger-ui.html", path) ||
                matcher.match("/swagger-resources/**", path) ||
                matcher.match("/webjars/**", path);

        log.info("[JWT FILTER] shouldNotFilter={}", shouldSkip);
        return shouldSkip;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        log.info("[JWT FILTER] doFilterInternal 실행됨 - path={}", request.getRequestURI());

        try {
            String token = resolveToken(request);
            log.info("[JWT FILTER] token={}", token != null ? "존재함" : "없음");

            // 🔹 토큰이 없는 경우 -> 인증 없이 다음 필터로
            if (token == null) {
                log.info("[JWT FILTER] 토큰 없음 - 다음 필터로 진행");
                filterChain.doFilter(request, response);
                return;
            }

            // 🔹 토큰이 있지만 유효하지 않으면 401 반환
            if (!jwtTokenProvider.validateToken(token)) {
                log.warn("[JWT FILTER] 유효하지 않은 JWT 토큰 - 401 반환");
                setErrorResponse(response, HttpStatus.UNAUTHORIZED, "유효하지 않은 JWT 토큰입니다.");
                return;
            }

            // 🔹 토큰이 유효하면 인증 컨텍스트 설정
            String email = jwtTokenProvider.getEmail(token);
            var userDetails = customUserDetailsService.loadUserByUsername(email);
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);

            filterChain.doFilter(request, response);
        } catch (Exception ex) {
            log.error("JWT 인증 중 오류 발생", ex);
            setErrorResponse(response, HttpStatus.UNAUTHORIZED, "JWT 인증 처리 중 오류가 발생했습니다.");
        }
    }

    /**
     * Authorization 헤더에서 토큰 추출
     */
    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");

        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        return null;
    }

    /**
     * 에러 응답 포맷
     */
    private void setErrorResponse(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType("application/json;charset=UTF-8");

        var errorBody = new ErrorResponse(message);
        String json = objectMapper.writeValueAsString(errorBody);

        response.getWriter().write(json);
        response.getWriter().flush();
        response.getWriter().close();
    }
}
