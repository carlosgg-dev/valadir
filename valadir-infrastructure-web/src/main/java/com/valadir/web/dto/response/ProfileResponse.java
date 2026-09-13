package com.valadir.web.dto.response;

public record ProfileResponse(
    String email,
    String fullName,
    String givenName,
    String language) {

}
