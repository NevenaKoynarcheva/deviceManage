package com.DeviceManage.deviceManage.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Вход и роли.
 * ADMIN  - вижда всичко и може да добавя, редактира, изтрива и управлява устройства.
 * VIEWER - само наблюдава (началния екран, списъка, детайлите и графиките).
 * Паролите се четат от application-local.properties, който не се качва в Git,
 * и се пазят в паметта само като BCrypt хеш.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // Правилата се проверяват отгоре надолу - важи първото съвпадение
                .requestMatchers("/login", "/css/**").permitAll()
                .requestMatchers("/logout").authenticated()
                .requestMatchers("/devices/new", "/devices/*/edit", "/devices/*/manage/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/**").hasRole("ADMIN") // всяка промяна
                .anyRequest().authenticated())
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/", true))
            .logout(logout -> logout
                .logoutSuccessUrl("/login?logout"));
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService users(PasswordEncoder encoder,
                                    @Value("${nms.admin.password}") String adminPassword,
                                    @Value("${nms.viewer.password}") String viewerPassword) {
        return new InMemoryUserDetailsManager(
                User.withUsername("admin").password(encoder.encode(adminPassword)).roles("ADMIN").build(),
                User.withUsername("viewer").password(encoder.encode(viewerPassword)).roles("VIEWER").build());
    }
}
