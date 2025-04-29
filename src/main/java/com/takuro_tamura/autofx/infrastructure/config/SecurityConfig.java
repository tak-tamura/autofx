package com.takuro_tamura.autofx.infrastructure.config;

import com.takuro_tamura.autofx.domain.service.UserService;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.expression.WebExpressionAuthorizationManager;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        final String localAccess = "hasIpAddress('127.0.0.1') or hasIpAddress('::1')";

        http
            .formLogin(form -> form
                .loginPage("/app/login")
                .loginProcessingUrl("/login")
                .successHandler((req, res, auth) -> res.setStatus(200))
                .failureHandler((req, res, auth) -> res.sendError(401))
                .permitAll()
            )
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/app/**", "/error", "/index.html", "**.png", "/api/auth/me").permitAll()
                .requestMatchers("/api/v1/candle/**").access(new WebExpressionAuthorizationManager(localAccess))
                .requestMatchers("/api/v1/trade/**").access(new WebExpressionAuthorizationManager(localAccess))
                .requestMatchers("/api/auth/register").access(new WebExpressionAuthorizationManager(localAccess))
                .requestMatchers(PathRequest.toStaticResources().atCommonLocations()).permitAll()
                .requestMatchers("/static/**", "manifest.json", "favicon.ico").permitAll()
                .anyRequest().authenticated()
            );
        return http.build();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider(
        UserService userService,
        PasswordEncoder passwordEncoder
    ) {
        final var authenticationProvider = new DaoAuthenticationProvider();
        authenticationProvider.setUserDetailsService(userService);
        authenticationProvider.setPasswordEncoder(passwordEncoder);
        return authenticationProvider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
