package com.jachwisunbae.member.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.jachwisunbae.common.exception.client.InvalidCommandException;
import com.jachwisunbae.common.exception.errorcode.ErrorCode;
import com.jachwisunbae.common.exception.server.UpstreamServiceException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

class RestClientGoogleOAuthGatewayTest {

    private static final String TOKEN_URI = "https://google.example.test/token";

    private MockRestServiceServer server;
    private RestClientGoogleOAuthGateway gateway;

    @BeforeEach
    void setUp() {
        final RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        gateway = new RestClientGoogleOAuthGateway(
                builder,
                new GoogleOAuthProperties(
                        "client-id",
                        "client-secret",
                        TOKEN_URI,
                        "https://google.example.test/certs",
                        List.of("https://app.example.test/callback")
                )
        );
    }

    @DisplayName("Google token endpoint의 ID Token만 로그인 경계 밖으로 전달한다")
    @Test
    void exchangeAuthorizationCode() {
        final MultiValueMap<String, String> expectedForm = new LinkedMultiValueMap<>();
        expectedForm.add("client_id", "client-id");
        expectedForm.add("client_secret", "client-secret");
        expectedForm.add("code", "authorization-code");
        expectedForm.add("code_verifier", "code-verifier");
        expectedForm.add("redirect_uri", "https://app.example.test/callback");
        expectedForm.add("grant_type", "authorization_code");
        server.expect(once(), requestTo(TOKEN_URI))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(content().formData(expectedForm))
                .andRespond(withSuccess(
                        "{\"access_token\":\"google-access-token\",\"id_token\":\"google-id-token\"}",
                        MediaType.APPLICATION_JSON
                ));

        final GoogleToken token = gateway.exchange(
                "authorization-code",
                "code-verifier",
                "https://app.example.test/callback"
        );

        assertThat(token.idToken()).isEqualTo("google-id-token");
        server.verify();
    }

    @DisplayName("Google token endpoint의 4xx 응답을 authorization code 오류로 변환한다")
    @Test
    void convertClientError() {
        server.expect(once(), requestTo(TOKEN_URI))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST));

        assertThatThrownBy(() -> gateway.exchange(
                "invalid-code",
                "code-verifier",
                "https://app.example.test/callback"
        )).isInstanceOfSatisfying(InvalidCommandException.class, exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.GOOGLE_AUTHORIZATION_CODE_INVALID)
        );
        server.verify();
    }

    @DisplayName("Google token endpoint의 5xx 응답을 상류 서비스 오류로 변환한다")
    @Test
    void convertServerError() {
        server.expect(once(), requestTo(TOKEN_URI))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertThatThrownBy(() -> gateway.exchange(
                "authorization-code",
                "code-verifier",
                "https://app.example.test/callback"
        )).isInstanceOfSatisfying(UpstreamServiceException.class, exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.GOOGLE_AUTHENTICATION_FAILED)
        );
        server.verify();
    }

    @DisplayName("Google 성공 응답에 ID Token이 없으면 상류 서비스 오류로 변환한다")
    @Test
    void rejectResponseWithoutIdToken() {
        server.expect(once(), requestTo(TOKEN_URI))
                .andRespond(withSuccess("{\"access_token\":\"google-access-token\"}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> gateway.exchange(
                "authorization-code",
                "code-verifier",
                "https://app.example.test/callback"
        )).isInstanceOfSatisfying(UpstreamServiceException.class, exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.GOOGLE_AUTHENTICATION_FAILED)
        );
        server.verify();
    }
}
