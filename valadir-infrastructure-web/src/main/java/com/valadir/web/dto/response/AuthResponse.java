package com.valadir.web.dto.response;

public record AuthResponse(
    String accessToken,
    String refreshToken) {

}
