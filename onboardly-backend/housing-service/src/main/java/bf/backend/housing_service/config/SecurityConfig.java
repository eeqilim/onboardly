package bf.backend.housing_service.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Value("${security.jwt.token.key}")
    private String jwtKey;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/actuator/health",
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(List.of("*"));
        configuration.setAllowedHeaders(List.of("*"));

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        return token -> {
            try {
                Jws<Claims> parsedToken = Jwts.parser()
                        .setSigningKey(jwtKey)
                        .parseClaimsJws(token);

                Claims claims = parsedToken.getBody();

                return Jwt.withTokenValue(token)
                        .headers(headers -> headers.putAll(parsedToken.getHeader()))
                        .claims(jwtClaims -> jwtClaims.putAll(claims))
                        .subject(claims.getSubject())
                        .issuedAt(claims.getIssuedAt().toInstant())
                        .expiresAt(claims.getExpiration().toInstant())
                        .build();
            } catch (Exception ex) {
                throw new JwtException("Invalid JWT token", ex);
            }
        };
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter scopesConverter = new JwtGrantedAuthoritiesConverter();

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            List<GrantedAuthority> roleAuthorities = extractRoleAuthorities(jwt);
            Collection<GrantedAuthority> scopeAuthorities = scopesConverter.convert(jwt);
            if (scopeAuthorities != null) {
                roleAuthorities.addAll(scopeAuthorities);
            }
            return roleAuthorities;
        });
        return converter;
    }

    private List<GrantedAuthority> extractRoleAuthorities(Jwt jwt) {
        List<?> roles = null;
        Object rawRoles = jwt.getClaim("roles");
        if (rawRoles instanceof List<?> rawRoleList) {
            roles = rawRoleList;
        } else if (rawRoles instanceof String roleName) {
            roles = List.of(roleName);
        }

        if (roles == null) {
            return new java.util.ArrayList<>();
        }

        return roles.stream()
                .map(this::extractRoleName)
                .filter(role -> role != null && !role.isBlank())
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toCollection(java.util.ArrayList::new));
    }

    private String extractRoleName(Object role) {
        if (role instanceof String roleName) {
            return roleName;
        }
        if (role instanceof Map<?, ?> roleMap && roleMap.get("authority") instanceof String authority) {
            return authority;
        }
        return null;
    }

}
