package com.vaxly.conversionservice.dtos;

public class CognitoResponseDto {
    private final String accessToken;
    private final long expiresIn; // in seconds

    public CognitoResponseDto(String accessToken, long expiresIn) {
        this.accessToken = accessToken;
        this.expiresIn = expiresIn;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public long getExpiresIn() {
        return expiresIn;
    }
}
