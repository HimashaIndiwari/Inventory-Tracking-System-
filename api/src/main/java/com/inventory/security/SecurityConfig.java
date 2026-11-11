package com.inventory.security;

import java.util.List;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.inventory.config.AppCorsProperties;
import com.inventory.service.UserProvisioningService;

/** Stateless bearer-token security (spec §4.2, §6.1). Authorization is enforced here, never only in the UI. */
@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(AppCorsProperties.class)
public class SecurityConfig {

    private static final String[] CATALOG = {"/api/products/**", "/api/categories/**", "/api/suppliers/**"};

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, FirebaseTokenVerifier verifier,
            UserProvisioningService provisioning, ProblemDetailWriter writer, AppCorsProperties cors) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .cors(c -> c.configurationSource(corsConfigurationSource(cors)))
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .logout(logout -> logout.disable())
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(new JsonAuthenticationEntryPoint(writer))
                        .accessDeniedHandler(new JsonAccessDeniedHandler(writer)))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/stock/adjust").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/stock/in", "/api/stock/out").authenticated()
                        .requestMatchers(HttpMethod.POST, CATALOG).hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, CATALOG).hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, CATALOG).hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/**").authenticated()
                        .requestMatchers("/error").permitAll()
                        .anyRequest().denyAll())
                .addFilterBefore(new FirebaseAuthFilter(verifier, provisioning, writer),
                        UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    private static CorsConfigurationSource corsConfigurationSource(AppCorsProperties cors) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(cors.allowedOrigins());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
