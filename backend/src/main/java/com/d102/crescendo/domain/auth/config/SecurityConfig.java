package com.d102.crescendo.domain.auth.config;

import com.d102.crescendo.domain.auth.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 전역 설정
 * - JWT 기반 인증 구조로 세션은 사용하지 않음
 * - Swagger / Auth / CORS Preflight(OPTIONS) 요청은 permitAll()
 * - 나머지 API는 JWT 인증 필요
 */
@Configuration
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // CSRF 비활성화 (JWT 기반이므로 필요 없음)
                .csrf(csrf -> csrf.disable())

                // CORS 기본 설정 허용
                .cors(Customizer.withDefaults())

                // 세션 사용 안 함 (JWT 기반 무상태)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 요청 인가 규칙
                .authorizeHttpRequests(auth -> auth
                        // 🔹 OPTIONS (CORS Preflight) 요청은 무조건 허용
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // 🔹 인증 불필요 엔드포인트
                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/swagger-resources/**",
                                "/webjars/**",
                                "/api/auth/**",
                                "/api/s3/**",
                                "/api/common/**",
                                "/api/test-es/**",
                                "/api/common/**",
                                "/api/ai/**"
                        ).permitAll()

                        // 🔹 그 외 요청은 인증 필요
                        .anyRequest().authenticated()
                )

                // 예외 핸들링
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((req, res, e) -> res.sendError(HttpServletResponse.SC_UNAUTHORIZED))
                        .accessDeniedHandler((req, res, e) -> res.sendError(HttpServletResponse.SC_FORBIDDEN))
                )

                // JWT 필터 등록 (UsernamePasswordAuthenticationFilter 전에 실행)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
