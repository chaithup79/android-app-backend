package com.curiq.api.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val firebaseTokenFilter: FirebaseTokenFilter
) {
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { auth ->
                auth
                    // Public endpoints
                    .requestMatchers("/api/v1/public/**", "/api/webhooks/revenuecat").permitAll()
                    // Secured endpoints
                    .requestMatchers("/api/v1/**").authenticated()
                    // Allow Spring Boot error fallback (prevents 404s masking 401s)
                    .requestMatchers("/error").permitAll()
                    // Everything else
                    .anyRequest().authenticated()
            }
            .addFilterBefore(firebaseTokenFilter, UsernamePasswordAuthenticationFilter::class.java)

        return http.build()
    }
}
