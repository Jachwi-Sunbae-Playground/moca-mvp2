package com.jachwisunbae.member.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.jachwisunbae.common.exception.client.InvalidCommandException;
import com.jachwisunbae.common.exception.errorcode.ErrorCode;
import com.jachwisunbae.common.exception.server.UpstreamServiceException;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class RestClientGoogleOAuthGateway implements GoogleOAuthGateway {

    private final RestClient restClient;
    private final GoogleOAuthProperties properties;

    public RestClientGoogleOAuthGateway(
            final RestClient.Builder restClientBuilder,
            final GoogleOAuthProperties properties
    ) {
        this.restClient = restClientBuilder.build();
        this.properties = properties;
    }

    @Override
    public GoogleToken exchange(
            final String authorizationCode,
            final String codeVerifier,
            final String redirectUri
    ) {
        final MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", properties.clientId());
        form.add("client_secret", properties.clientSecret());
        form.add("code", authorizationCode);
        form.add("code_verifier", codeVerifier);
        form.add("redirect_uri", redirectUri);
        form.add("grant_type", "authorization_code");

        try {
            final GoogleTokenResponse response = restClient.post()
                    .uri(properties.tokenUri())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (request, upstreamResponse) -> {
                        throw new InvalidCommandException(ErrorCode.GOOGLE_AUTHORIZATION_CODE_INVALID);
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (request, upstreamResponse) -> {
                        throw new UpstreamServiceException(ErrorCode.GOOGLE_AUTHENTICATION_FAILED);
                    })
                    .body(GoogleTokenResponse.class);

            if (response == null || response.idToken() == null || response.idToken().isBlank()) {
                throw new UpstreamServiceException(ErrorCode.GOOGLE_AUTHENTICATION_FAILED);
            }
            return new GoogleToken(response.idToken());
        } catch (InvalidCommandException | UpstreamServiceException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new UpstreamServiceException(ErrorCode.GOOGLE_AUTHENTICATION_FAILED, exception);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GoogleTokenResponse(@JsonProperty("id_token") String idToken) {
    }
}
