package com.dispensary.dms.security;

import com.dispensary.dms.config.AppProperties;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

/**
 * HMAC-SHA256 JWT encoder/decoder built from {@code APP_JWT_SECRET} (spec section 9.2).
 * The secret is Base64-decoded; {@link com.dispensary.dms.config.StartupValidator} guarantees
 * at least 32 bytes.
 */
@Configuration(proxyBeanMethods = false)
public class JwtConfig {

    private final byte[] secret;
    private final String issuer;

    public JwtConfig(AppProperties props) {
        this.secret = Base64.getDecoder().decode(props.jwt().secret());
        this.issuer = props.jwt().issuer();
    }

    private SecretKeySpec key() {
        return new SecretKeySpec(secret, "HmacSHA256");
    }

    @Bean
    public JwtEncoder jwtEncoder() {
        return new NimbusJwtEncoder(new ImmutableSecret<>(key()));
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key())
                .macAlgorithm(org.springframework.security.oauth2.jose.jws.MacAlgorithm.HS256)
                .build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuer));
        return decoder;
    }
}
