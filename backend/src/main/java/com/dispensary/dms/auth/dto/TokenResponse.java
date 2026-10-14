package com.dispensary.dms.auth.dto;

public record TokenResponse(String accessToken, String tokenType, long expiresIn, CurrentUser user) {

    public static TokenResponse bearer(String accessToken, long expiresIn, CurrentUser user) {
        return new TokenResponse(accessToken, "Bearer", expiresIn, user);
    }
}
