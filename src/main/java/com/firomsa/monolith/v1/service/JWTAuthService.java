package com.firomsa.monolith.v1.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jose.jws.JwsAlgorithms;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class JWTAuthService {

    private final int JWT_DURATION = 15;
    private final int REFRESH_TOKEN_DURATION = 15;
    private final JwtEncoder encoder;

    public String generateToken(Authentication authentication) {
        Instant now = Instant.now();
        String scope = authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(" "));

        JwtClaimsSet claims = JwtClaimsSet.builder().issuer("self").issuedAt(now)
                .expiresAt(now.plus(JWT_DURATION, ChronoUnit.MINUTES))
                .subject(authentication.getName()).claim("scope", scope).build();

        JwsHeader jwsHeader = JwsHeader.with(() -> JwsAlgorithms.HS256).build();

        return this.encoder.encode(JwtEncoderParameters.from(jwsHeader, claims)).getTokenValue();
    }

    public String generateRefreshToken(Authentication authentication) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder().issuer("self").issuedAt(now)
                .expiresAt(now.plus(REFRESH_TOKEN_DURATION, ChronoUnit.DAYS))
                .subject(authentication.getName()).claim("type", "REFRESH").build();

        JwsHeader jwsHeader = JwsHeader.with(() -> JwsAlgorithms.HS256).build();

        return this.encoder.encode(JwtEncoderParameters.from(jwsHeader, claims)).getTokenValue();
    }
}
