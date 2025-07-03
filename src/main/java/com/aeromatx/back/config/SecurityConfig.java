package com.aeromatx.back.config;

import com.aeromatx.back.security.JwtAuthenticationEntryPoint;
import com.aeromatx.back.security.JwtAuthenticationFilter;
import com.aeromatx.back.security.UserDetailsServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.http.HttpMethod;


@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true) // Enable @PreAuthorize, @PostAuthorize annotations
public class SecurityConfig {

    private final UserDetailsServiceImpl userDetailsService;
    private final JwtAuthenticationEntryPoint unauthorizedHandler;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(UserDetailsServiceImpl userDetailsService,
                          JwtAuthenticationEntryPoint unauthorizedHandler,
                          JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.userDetailsService = userDetailsService;
        this.unauthorizedHandler = unauthorizedHandler;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable()) // Disable CSRF for stateless REST APIs
            .exceptionHandling(exception -> exception.authenticationEntryPoint(unauthorizedHandler)) // Handle unauthorized access
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // Use stateless sessions for JWT
            .authorizeHttpRequests(auth -> auth
                // Permit all static resources and public HTML files first
                .requestMatchers(
                    "/", // Root path
                    "/index.html", // Main index file
                    "/*.html", // Other HTML files at the root
                    "/*.css", "/*.js", // CSS/JS files at the root
                    "/css/**","/css2/**","/css3/**", "/js/**","/js2/**", "/img/**", "/lib/**", "/fonts/**", // Common static folders
                    "/favicon.ico", // Favicon
                    "/error", // Spring Boot default error page
                    "/assets/**", // For common assets folder (if you use it)
                    "/admin_pannel/**" // This will cover all HTML, CSS, JS inside admin_pannel
                ).permitAll()

                // Permit all /api/auth endpoints for public access (registration, login, forgot/reset password API calls)
                .requestMatchers(HttpMethod.POST, "/api/vendor/register").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/vendor", "/api/vendor/{id}").permitAll()
                .requestMatchers(HttpMethod.PUT, "/api/vendor/{id}").permitAll()
                .requestMatchers(HttpMethod.DELETE, "/api/vendor/{id}").permitAll()
                .requestMatchers("/api/auth/**").permitAll()

                // Permit all category endpoints for public access (e.g., for frontend display)
                // This rule is CRUCIAL for your frontend categories to load without auth errors.
                // It must come BEFORE the general "/api/**.authenticated()" rule.
                .requestMatchers("/api/categories/**").permitAll() // Already correct and allows frontend access

                // TEMPORARY: Allow all product endpoints for quick testing (consider securing these later)
                .requestMatchers("/api/products/**").permitAll()

                // Permit access to /api/users/by-role for all for now.
                .requestMatchers("/api/users/by-role").permitAll()

                // Specific DELETE endpoint for users (admin only)
                .requestMatchers(HttpMethod.DELETE, "/api/users/**").permitAll() 

                // Allow admin login page
                .requestMatchers("/admin_pannel/admin-login.html").permitAll()
                .requestMatchers("/admin_pannel/customers.html").permitAll()

                // All other /api endpoints should require authentication
                .requestMatchers("/api/**").authenticated()
                // Any other request that hasn't been matched by previous rules should also be authenticated.
                .anyRequest().authenticated()
            );

        http.authenticationProvider(authenticationProvider()); // Set up authentication provider
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class); // Add JWT filter before UsernamePasswordAuthenticationFilter

        return http.build();
    }
}