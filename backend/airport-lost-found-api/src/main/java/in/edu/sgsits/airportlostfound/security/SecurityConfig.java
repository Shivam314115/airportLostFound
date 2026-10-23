package in.edu.sgsits.airportlostfound.security;

import java.util.List;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    /**
     * Provides Spring Security's configured authentication manager.
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration) throws Exception {

        return configuration.getAuthenticationManager();
    }

    /**
     * Persists authenticated security contexts in HTTP sessions.
     */
    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    /**
     * Protects against session fixation during authentication.
     */
    @Bean
    public SessionAuthenticationStrategy sessionAuthenticationStrategy() {
        return new ChangeSessionIdAuthenticationStrategy();
    }

    /**
     * Configures CORS for local dev servers and production deployments.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of(
                "http://localhost:*",
                "http://127.0.0.1:*",
                "https://*.vercel.app",
                "https://*.railway.app",
                "https://*.render.com"
        ));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            SecurityContextRepository securityContextRepository)
            throws Exception {

        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // Keep CSRF protection enabled for browser-based sessions, ignoring stateless photo uploads.
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/api/photos/upload")
                .csrfTokenRepository(
                    new HttpSessionCsrfTokenRepository()
                )
            )

            // Use the same repository for loading and saving authentication.
            .securityContext(context -> context
                .securityContextRepository(securityContextRepository)
            )

            // Define public and protected endpoints.
            .authorizeHttpRequests(authorize -> authorize
                .dispatcherTypeMatchers(DispatcherType.FORWARD, DispatcherType.ERROR).permitAll()

                .requestMatchers(
                    "/",
                    "/index.html",
                    "/discover",
                    "/my-reports",
                    "/matches",
                    "/staff",
                    "/staff/**",
                    "/assets/**",
                    "/models/**",
                    "/plane.glb",
                    "/*.ico",
                    "/*.svg",
                    "/*.png",
                    "/*.json"
                ).permitAll()

                .requestMatchers(
                    HttpMethod.POST,
                    "/api/passengers",
                    "/api/auth/login",
                    "/api/photos/upload"
                ).permitAll()

                .requestMatchers(
                    HttpMethod.GET,
                    "/api/auth/csrf",
                    "/api/reference-data",
                    "/api/lost-reports",
                    "/api/photos/**",
                    "/api/items/*/photos",
                    "/api/staff/found-items/*/photos"
                ).permitAll()

                .requestMatchers(
                    HttpMethod.HEAD,
                    "/api/photos/**"
                ).permitAll()

                .requestMatchers("/error").permitAll()

                .anyRequest().authenticated()
            )

            // Use our custom JSON login endpoint, not a generated login page.
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable())

            // Maintain server-side HTTP sessions when authentication is needed.
            .sessionManagement(session -> session
                .sessionCreationPolicy(
                    SessionCreationPolicy.IF_REQUIRED
                )
                .sessionFixation(fixation ->
                    fixation.changeSessionId()
                )
            )

            // Invalidate the session on logout.
            .logout(logout -> logout
                .logoutUrl("/api/auth/logout")
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies("JSESSIONID")
                .logoutSuccessHandler((request, response, authentication) ->
                    response.setStatus(
                        HttpServletResponse.SC_NO_CONTENT
                    )
                )
            );

        return http.build();
    }
}
