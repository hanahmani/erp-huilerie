package com.huilerie.erp.security;

import com.huilerie.erp.users.AppUser;
import com.huilerie.erp.users.UserRepository;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Transforme un JWT valide en utilisateur authentifié, en relisant l'utilisateur en base :
 * compte désactivé, mot de passe changé ou rôle retiré prennent effet immédiatement.
 */
@Component
public class DbJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    /** Autorité unique tant que l'utilisateur doit changer son mot de passe provisoire. */
    public static final String PASSWORD_CHANGE_REQUIRED = "PASSWORD_CHANGE_REQUIRED";

    private final UserRepository userRepository;

    public DbJwtAuthenticationConverter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        AppUser user = userRepository.findByUsername(jwt.getSubject())
                .filter(AppUser::isActive)
                .orElseThrow(() -> new DisabledException("Compte inactif ou introuvable."));

        Object version = jwt.getClaim(JwtService.VERSION_CLAIM);
        if (!(version instanceof Number n) || n.intValue() != user.getTokenVersion()) {
            throw new CredentialsExpiredException("Session expirée. Reconnectez-vous.");
        }

        List<GrantedAuthority> authorities = new ArrayList<>();
        if (user.isMustChangePassword()) {
            authorities.add(new SimpleGrantedAuthority(PASSWORD_CHANGE_REQUIRED));
        } else {
            user.permissionCodes().forEach(p -> authorities.add(new SimpleGrantedAuthority(p)));
            user.roleCodes().forEach(r -> authorities.add(new SimpleGrantedAuthority("ROLE_" + r)));
        }
        return new JwtAuthenticationToken(jwt, authorities, user.getUsername());
    }
}
