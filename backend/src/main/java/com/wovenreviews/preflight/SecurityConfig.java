package com.wovenreviews.preflight;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Everything is open.
 *
 * <p>Spring Security is on the classpath because the real application depends on it and this
 * project exists to pull those dependencies down ahead of time — but a preflight that asked for
 * credentials would be checking the wrong thing. Nothing here is a template for the real app.
 */
@Configuration
class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http.csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(requests -> requests.anyRequest().permitAll())
                .build();
    }
}
