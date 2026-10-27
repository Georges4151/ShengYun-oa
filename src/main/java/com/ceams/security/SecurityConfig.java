package com.ceams.security;

import com.ceams.config.CorsConfig;
import com.ceams.constant.RoleConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

        @Autowired
        private JwtAuthenticationFilter jwtAuthenticationFilter;

        @Autowired
        private CorsConfig corsConfig;

        @Bean
        public BCryptPasswordEncoder bCryptPasswordEncoder() {
                return new BCryptPasswordEncoder();
        }

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
                http
                                .cors(cors -> cors.configurationSource(corsConfig.corsConfigurationSource()))
                                .authorizeHttpRequests(authorizeRequests -> authorizeRequests
                                                .requestMatchers("/", "/login", "/register", "/css/**", "/js/**",
                                                                "/images/**")
                                                .permitAll()
                                                .requestMatchers("/api/auth/**").permitAll()
                                                .requestMatchers("/api/files/**").permitAll()
                                                .requestMatchers("/api/dashboard/**").permitAll()
                                                .requestMatchers("/api/**").authenticated()
                                                .anyRequest().authenticated())
                                .formLogin(formLogin -> formLogin
                                                .loginPage("/login")
                                                .defaultSuccessUrl("/dashboard")
                                                .permitAll()
                                                .loginProcessingUrl("/login"))
                                .logout(logout -> logout
                                                .logoutUrl("/logout")
                                                .logoutSuccessUrl("/login")
                                                .permitAll())
                                .csrf(csrf -> csrf
                                                .ignoringRequestMatchers("/api/**")  // 对所有API路径禁用CSRF（包括移动端和远程客户端）
                                                .csrfTokenRepository(
                                                                org.springframework.security.web.csrf.CookieCsrfTokenRepository
                                                                                .withHttpOnlyFalse()))
                                .exceptionHandling(exception -> exception
                                                .authenticationEntryPoint((request, response, authException) -> {
                                                        if (request.getRequestURI().startsWith("/api/")) {
                                                                response.setContentType(
                                                                                "application/json;charset=UTF-8");
                                                                response.setStatus(401);
                                                                response.getWriter().write(
                                                                                "{\"success\":false,\"message\":\"未授权访问\"}");
                                                        } else {
                                                                response.sendRedirect("/login");
                                                        }
                                                })
                                                .accessDeniedHandler((request, response, accessDeniedException) -> {
                                                        String requestURI = request.getRequestURI();
                                                        if (requestURI.startsWith("/api/")) {
                                                                response.setContentType(
                                                                                "application/json;charset=UTF-8");
                                                                response.setStatus(403);
                                                                response.getWriter().write(
                                                                                "{\"success\":false,\"message\":\"权限不足\"}");
                                                        } else {
                                                                response.sendRedirect("/login?error=access_denied");
                                                        }
                                                }))
                                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }
}