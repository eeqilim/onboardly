package com.example.authserver.security;

import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

import java.util.Date;

@Component
public class JwtProvider {
    @Value("${security.jwt.token.key}")
    private String key;

    @Value("${security.jwt.token.expiration:86400000}")
    private Long expiration;

    public String createToken(UserDetails userDetails){
        Claims claims = Jwts.claims().setSubject(userDetails.getUsername());
        claims.put("roles", userDetails.getAuthorities());
        claims.put("userId", ((AuthUserDetail) userDetails).getId());
        claims.put("email", ((AuthUserDetail) userDetails).getEmail());

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(SignatureAlgorithm.HS256, key)
                .compact();
    }
}

