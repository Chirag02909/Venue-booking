    package com.demo.config;

    import com.demo.service.CustomUserDetailsService;
    import org.springframework.beans.factory.annotation.Autowired;
    import org.springframework.context.annotation.Bean;
    import org.springframework.context.annotation.Configuration;
    import org.springframework.security.authentication.AuthenticationManager;
    import org.springframework.security.authentication.AuthenticationProvider;
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

    @Configuration
    @EnableWebSecurity
    @EnableMethodSecurity
    public class SecurityConfig {

        @Autowired
        private JwtAuthFilter jwtAuthFilter;

        @Autowired
        private CustomUserDetailsService userDetailsService;

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            http
                    .csrf(csrf -> csrf.disable())
//                    .cors(cors -> cors.disable())
                    .cors(cors -> cors.configurationSource(request -> {
                        var corsConfig = new org.springframework.web.cors.CorsConfiguration();
                        corsConfig.setAllowedOrigins(java.util.List.of("http://localhost:8081"));
                        corsConfig.setAllowedMethods(java.util.List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
                        corsConfig.setAllowedHeaders(java.util.List.of("Authorization", "Content-Type"));
                        corsConfig.setAllowCredentials(true);
                        return corsConfig;
                    }))
                    .authorizeHttpRequests(auth -> auth
                            .requestMatchers("/api/auth/**").permitAll()
                            .requestMatchers("/api/venues", "/api/venues/**").permitAll()
                            .requestMatchers("/api/razorpay/webhook").permitAll() // Webhook doesn't need JWT
                            .requestMatchers("/api/razorpay/**").authenticated()
                            .requestMatchers("/api/owner/**").authenticated()
                            .requestMatchers("/api/bookings/**").permitAll()
                            .requestMatchers("/api/payments/**").authenticated()
                            .requestMatchers("/api/reviews/**").permitAll()
//                            .requestMatchers("/api/analytics/**").hasAnyRole("ADMIN", "OWNER")
                            .anyRequest().authenticated()
                    )
                    .sessionManagement(session -> session
                            .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                    )
                    .authenticationProvider(authenticationProvider())
                    .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

            return http.build();
        }

        @Bean
        public AuthenticationProvider authenticationProvider() {
            DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
            provider.setUserDetailsService(userDetailsService);
            provider.setPasswordEncoder(passwordEncoder());
            return provider;
        }

        @Bean
        public PasswordEncoder passwordEncoder() {
            return new BCryptPasswordEncoder();
        }

        @Bean
        public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
            return config.getAuthenticationManager();
        }
    }