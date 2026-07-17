package com.saryom.foodservice.auth;

/** Identity extracted from a verified Firebase ID token. */
public record VerifiedUser(String uid, String email, String displayName) {
}
