package com.example.applicationservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class JwtProvider {

    @Value("${security.jwt.token.key}")
    private String key;

    public Optional<AuthUserDetail> resolveToken(HttpServletRequest request) {
        String prefixedToken = request.getHeader("Authorization");

        if (prefixedToken == null || !prefixedToken.startsWith("Bearer ")) {
            return Optional.empty();
        }

        String token = prefixedToken.substring(7);
        Claims claims = Jwts.parser().setSigningKey(key).parseClaimsJws(token).getBody();
        String username = claims.getSubject();
        Object rawUserId = claims.get("userId");

        Long userId = null;
        if (rawUserId instanceof Number number) {
            userId = number.longValue();
        } else if (rawUserId instanceof String userIdString) {
            userId = Long.parseLong(userIdString);
        }

        List<LinkedHashMap<String, String>> roles = (List<LinkedHashMap<String, String>>) claims.get("roles");
        List<GrantedAuthority> authorities = roles.stream()
                .map(p -> new SimpleGrantedAuthority(p.get("authority")))
                .collect(Collectors.toList());

        return Optional.of(AuthUserDetail.builder()
                .userId(userId)
                .username(username)
                .authorities(authorities)
                .accountNonExpired(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
                .enabled(true)
                .build());
    }
}
