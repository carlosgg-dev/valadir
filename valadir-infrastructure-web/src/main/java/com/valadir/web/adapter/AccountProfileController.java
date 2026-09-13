package com.valadir.web.adapter;

import com.valadir.application.command.UpdateProfileCommand;
import com.valadir.application.port.in.GetProfileUseCase;
import com.valadir.application.port.in.UpdateProfileUseCase;
import com.valadir.application.result.ProfileResult;
import com.valadir.web.config.ApiRoutes;
import com.valadir.web.dto.request.UpdateProfileRequest;
import com.valadir.web.dto.response.ProfileResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiRoutes.Auth.BASE)
class AccountProfileController {

    private final GetProfileUseCase getProfileUseCase;
    private final UpdateProfileUseCase updateProfileUseCase;

    AccountProfileController(GetProfileUseCase getProfileUseCase, UpdateProfileUseCase updateProfileUseCase) {

        this.getProfileUseCase = getProfileUseCase;
        this.updateProfileUseCase = updateProfileUseCase;
    }

    @GetMapping(ApiRoutes.Auth.Account.PROFILE)
    ProfileResponse getProfile(@AuthenticationPrincipal Jwt jwt) {

        ProfileResult result = getProfileUseCase.getProfile(jwt.getSubject());
        return new ProfileResponse(result.email(), result.fullName(), result.givenName(), result.language());
    }

    @PutMapping(ApiRoutes.Auth.Account.PROFILE)
    ProfileResponse updateProfile(@Valid @RequestBody UpdateProfileRequest request, @AuthenticationPrincipal Jwt jwt) {

        var command = new UpdateProfileCommand(jwt.getSubject(), request.fullName(), request.givenName(), request.language());
        ProfileResult result = updateProfileUseCase.update(command);
        return new ProfileResponse(result.email(), result.fullName(), result.givenName(), result.language());
    }
}
