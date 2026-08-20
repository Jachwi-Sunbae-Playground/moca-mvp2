package com.jachwisunbae.auth.google;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GoogleTokenResponse(
        @JsonProperty("id_token") String idToken) {
}
