package com.jachwisunbae.auth.google;

import com.jachwisunbae.common.exception.BusinessException;
import com.jachwisunbae.common.exception.DomainErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Slf4j
@Component
public class GoogleOAuthClient {

    private final RestClient restClient = RestClient.create("https://oauth2.googleapis.com");
    private final String clientId;
    private final String clientSecret;

    public GoogleOAuthClient(@Value("${auth.google.client-id}") String clientId,
            @Value("${auth.google.client-secret}") String clientSecret) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    public GoogleTokenResponse exchange(String code, String verifier, String redirectUri) {
        try {
            return requestToken(createTokenForm(code, verifier, redirectUri));
        } catch (RestClientResponseException exception) {
            log.warn(
                    "Google token exchange failed: status={}, errorType={}",
                    exception.getStatusCode().value(),
                    extractErrorType(exception.getResponseBodyAsString()));
            throw authenticationFailed();
        } catch (Exception exception) {
            log.warn("Google token exchange failed: type={}", exception.getClass().getSimpleName());
            throw authenticationFailed();
        }
    }

    private String extractErrorType(String responseBody) {
        if (responseBody == null) {
            return "unknown";
        }
        for (String type : new String[] {
                "invalid_grant", "invalid_client", "invalid_request", "unauthorized_client"
        }) {
            if (responseBody.contains(type)) {
                return type;
            }
        }
        return "unknown";
    }

    private BusinessException authenticationFailed() {
        return new BusinessException(
                DomainErrorCode.GOOGLE_AUTHENTICATION_FAILED,
                "Google authorization code 교환에 실패했습니다.");
    }

    private LinkedMultiValueMap<String, String> createTokenForm(
            String code,
            String verifier,
            String redirectUri) {
        var form = new LinkedMultiValueMap<String, String>();
        form.add("code", code);
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("code_verifier", verifier);
        form.add("redirect_uri", redirectUri);
        form.add("grant_type", "authorization_code");
        return form;
    }

    private GoogleTokenResponse requestToken(LinkedMultiValueMap<String, String> form) {
        return restClient.post()
                .uri("/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(GoogleTokenResponse.class);
    }
}
