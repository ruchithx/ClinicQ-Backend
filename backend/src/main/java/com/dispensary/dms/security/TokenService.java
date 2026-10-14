package com.dispensary.dms.security;

import com.dispensary.dms.config.AppProperties;
import com.dispensary.dms.common.time.BusinessClock;
import com.dispensary.dms.user.Role;
import com.dispensary.dms.user.UserEntity;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Issues short-lived access-token JWTs (spec section 9.2).
 */
@Service
public class TokenService {

    private final JwtEncoder encoder;
    private final BusinessClock clock;
    private final String issuer;
    private final Duration accessTtl;

    public TokenService(JwtEncoder encoder, BusinessClock clock, AppProperties props) {
        this.encoder = encoder;
        this.clock = clock;
        this.issuer = props.jwt().issuer();
        this.accessTtl = props.jwt().accessTokenTtl();
    }

    public record AccessToken(String value, long expiresInSeconds) {
    }

    public AccessToken issueAccessToken(UserEntity user) {
        Instant now = clock.now();
        Instant expiry = now.plus(accessTtl);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .issuedAt(now)
                .expiresAt(expiry)
                .subject(user.getId().toString())
                .id(UUID.randomUUID().toString())
                .claim("username", user.getUsername())
                .claim("roles", user.getRoles().stream().map(Role::name).toList())
                .claim("pwd_change", user.isMustChangePassword())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new AccessToken(token, accessTtl.toSeconds());
    }
}
