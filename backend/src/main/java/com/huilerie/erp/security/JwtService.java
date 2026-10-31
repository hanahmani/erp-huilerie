package com.huilerie.erp.security;

import com.huilerie.erp.config.JwtConfig;
import com.huilerie.erp.users.AppUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

/**
 * Émet les jetons de session. Le jeton ne contient PAS les permissions :
 * elles sont relues en base à chaque requête, donc un retrait de rôle est immédiat.
 */
@Service
public class JwtService {

    public static final String VERSION_CLAIM = "ver";
    /** Session longue choisie à la connexion (« Se souvenir de moi »), conservée au renouvellement du jeton. */
    public static final String REMEMBER_CLAIM = "rem";

    private final JwtEncoder encoder;
    private final Duration ttl;
    private final Duration rememberTtl;

    public JwtService(JwtEncoder encoder,
                      @Value("${app.jwt.expiration-minutes}") long expirationMinutes,
                      @Value("${app.jwt.remember-expiration-minutes}") long rememberExpirationMinutes) {
        this.encoder = encoder;
        this.ttl = Duration.ofMinutes(expirationMinutes);
        this.rememberTtl = Duration.ofMinutes(rememberExpirationMinutes);
    }

    public String issue(AppUser user, boolean remember) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(JwtConfig.ISSUER)
                .issuedAt(now)
                .expiresAt(now.plus(ttl(remember)))
                .subject(user.getUsername())
                .claim(VERSION_CLAIM, user.getTokenVersion())
                .claim(REMEMBER_CLAIM, remember)
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    public long ttlSeconds(boolean remember) {
        return ttl(remember).toSeconds();
    }

    private Duration ttl(boolean remember) {
        return remember ? rememberTtl : ttl;
    }
}
