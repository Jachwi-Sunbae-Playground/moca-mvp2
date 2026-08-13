package com.jachwisunbae.member.client;

public interface GoogleIdTokenVerifier {

    VerifiedGoogleProfile verify(String idToken, String expectedNonce);
}
