package com.jachwisunbae.member.client;

import java.time.Clock;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

@Configuration
public class GoogleOAuthClientConfig {

    @Bean
    @Qualifier("googleJwtDecoder")
    public JwtDecoder googleJwtDecoder(final GoogleOAuthProperties properties, final Clock clock) {
        final NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(properties.jwkSetUri()).build();
        decoder.setJwtValidator(new GoogleIdentityTokenValidator(properties.clientId(), clock));
        return decoder;
    }
}
