package com.factoreal.backend.global.config;

import com.factoreal.backend.global.security.application.CustomUserDetailsService;
import com.factoreal.backend.global.security.application.UserAgentRequestMatcher;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfigurationSource;


@EnableWebSecurity
@Configuration // @EnableWebSecurity가 포함하고 있지만 명시적으로 추가하는 것이 좋음
@AllArgsConstructor
public class SecurityConfig {
    private final CorsConfigurationSource corsConfigurationSource;
    private final CustomUserDetailsService customUserDetailsService;
    public static final String[]  ALLOWED_URLS = {
        "/api/auth/**", // 이 URL은 이제 Spring Security의 formLogin이 아닌, 직접 구현한 로그인 API를 위한 것
        "/ws/**",
        "/v3/api-docs/**",
        "/swagger-ui/**",
        "/swagger-ui.html"
    };

    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity httpSecurity) throws Exception {
        AuthenticationManagerBuilder authenticationManagerBuilder = httpSecurity.getSharedObject(AuthenticationManagerBuilder.class);
        authenticationManagerBuilder.userDetailsService(customUserDetailsService).passwordEncoder(bCryptPasswordEncoder());
        return authenticationManagerBuilder.build();
    }
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .httpBasic(AbstractHttpConfigurer::disable) // HTTP Basic 인증 비활성화 (폼 로그인과 별개)
            .csrf(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable) // 폼 로그인 비활성화
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
//            .cors(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session
                .maximumSessions(8)
                .expiredUrl("/") // 세션 만료 시 이동할 URL
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS,"/**").permitAll() // CORS preflight 요청 허용
                .requestMatchers(ALLOWED_URLS).permitAll() // 명시적으로 허용된 URL
                .requestMatchers(new UserAgentRequestMatcher("swagger")).permitAll()// swagger로 요청하는 것들은 모두 허용
                .anyRequest().authenticated() // 나머지 모든 요청은 인증 필요
            )
            // 로그아웃 설정 (필요하다면 주석 해제)
            .logout(logout -> logout
                .logoutUrl("/api/auth/logout")
                .logoutSuccessHandler((request, response, authentication) -> {
                    response.setStatus(HttpServletResponse.SC_OK);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"message\": \"Logout successful\"}");
                })
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID") // JSESSIONID 외 다른 쿠키도 있다면 명시
                .permitAll()
            )
            // 예외 처리 핸들러 설정 (매우 중요!)
            .exceptionHandling(exception -> exception
                .authenticationEntryPoint((request, response, authException) -> {
                    // 인증되지 않은 사용자가 보호된 리소스에 접근 시 호출됨
                    response.setContentType("application/json");
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401 응답
                    response.getWriter().write("{\"error\": \"Unauthorized\", \"message\": \"" + authException.getMessage() + "\"}");
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    // 인증은 되었으나, 접근 권한이 없는 리소스에 접근 시 호출됨
                    response.setContentType("application/json");
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN); // 403 응답 (또는 요구사항에 따라 401)
                    response.getWriter().write("{\"error\": \"Forbidden\", \"message\": \"" + accessDeniedException.getMessage() + "\"}");
                })
            ); // .build()는 HttpSecurity 체인의 마지막에 한 번만 호출됩니다.

        return http.build(); // 최종적으로 SecurityFilterChain 빌드
    }
    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }
    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.ignoring().requestMatchers("/ws/**"); // 웹소켓에는 security 무시
    }

    // AuthenticationManager 빈은 이전 답변에서처럼 필요하다면 추가합니다.
    // (만약 username/password 인증을 위한 커스텀 로직이 있다면 필요)
    // import org.springframework.security.authentication.AuthenticationManager;
    // import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
    // @Bean
    // public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
    //     return config.getAuthenticationManager();
    // }
}