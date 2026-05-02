package com.valadir.web.exception;

import com.valadir.application.exception.ApplicationException;
import com.valadir.common.error.ErrorCode;
import com.valadir.common.exception.InfrastructureException;
import com.valadir.domain.exception.AccountLockedException;
import java.time.Duration;
import com.valadir.domain.exception.DomainException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {

        mockMvc = MockMvcBuilders
            .standaloneSetup(new StubController())
            .setControllerAdvice(new GlobalExceptionHandler(new HttpStatusResolver()))
            .build();
    }

    @Test
    void handleMethodArgumentNotValid_blankField_returns400WithFieldErrors() throws Exception {

        mockMvc.perform(post("/validated")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_FIELD.getCode()))
            .andExpect(jsonPath("$.errors").isArray())
            .andExpect(jsonPath("$.errors[0].field").value("name"))
            .andExpect(jsonPath("$.errors[0].message").exists());
    }

    @Test
    void handleExceptionInternal_unsupportedMethod_preservesStatusWithSysCode() throws Exception {

        mockMvc.perform(delete("/domain"))
            .andExpect(status().isMethodNotAllowed())
            .andExpect(jsonPath("$.code").value(ErrorCode.INTERNAL_SERVER_ERROR.getCode()));
    }

    @Test
    void handleDomain_domainException_returns400WithCode() throws Exception {

        mockMvc.perform(get("/domain"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(ErrorCode.INSECURE_PASSWORD.getCode()));
    }

    @Test
    void handleApplication_validationCategory_returns400() throws Exception {

        mockMvc.perform(get("/application/validation"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_PASSWORD.getCode()));
    }

    @Test
    void handleApplication_conflictCategory_returns409() throws Exception {

        mockMvc.perform(get("/application/conflict"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value(ErrorCode.EMAIL_ALREADY_EXISTS.getCode()));
    }

    @Test
    void handleApplication_unauthorizedCategory_returns401() throws Exception {

        mockMvc.perform(get("/application/unauthorized"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value(ErrorCode.CREDENTIAL_INTEGRITY_ERROR.getCode()));
    }

    @Test
    void handleApplication_forbiddenCategory_returns403() throws Exception {

        mockMvc.perform(get("/application/forbidden"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value(ErrorCode.ACCESS_DENIED.getCode()));
    }

    @Test
    void handleApplication_rateLimitedCategory_returns429() throws Exception {

        mockMvc.perform(get("/application/rate-limited"))
            .andExpect(status().isTooManyRequests())
            .andExpect(jsonPath("$.code").value(ErrorCode.RATE_LIMIT_EXCEEDED.getCode()));
    }

    @Test
    void handleApplication_serverErrorCategory_returns500() throws Exception {

        mockMvc.perform(get("/application/server-error"))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.code").value(ErrorCode.TOKEN_REVOCATION_FAILED.getCode()));
    }

    @Test
    void handleAccountLocked_accountLockedException_returns429WithRetryAfterHeader() throws Exception {

        mockMvc.perform(get("/account-locked"))
            .andExpect(status().isTooManyRequests())
            .andExpect(header().string("Retry-After", "30"))
            .andExpect(jsonPath("$.code").value(ErrorCode.ACCOUNT_TEMPORARILY_LOCKED.getCode()));
    }

    @Test
    void handleInfrastructure_infrastructureException_returns503WithInfraCode() throws Exception {

        mockMvc.perform(get("/infrastructure"))
            .andExpect(status().isServiceUnavailable())
            .andExpect(jsonPath("$.code").value(ErrorCode.INFRASTRUCTURE_UNAVAILABLE.getCode()));
    }

    @Test
    void handleUnexpected_runtimeException_returns500WithSysCode() throws Exception {

        mockMvc.perform(get("/unexpected"))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.code").value(ErrorCode.INTERNAL_SERVER_ERROR.getCode()));
    }

    // --- stub controller ---

    @RestController
    static class StubController {

        @PostMapping("/validated")
        @SuppressWarnings("unused")
        void validated(@RequestBody @Valid ValidatedBody body) {

            // stub — never reached; Spring rejects the request before this method executes
        }

        @GetMapping("/domain")
        void domain() {

            throw new DomainException("domain error", ErrorCode.INSECURE_PASSWORD);
        }

        @GetMapping("/application/validation")
        void applicationValidation() {

            throw new ApplicationException("invalid password", ErrorCode.INVALID_PASSWORD);
        }

        @GetMapping("/application/conflict")
        void applicationConflict() {

            throw new ApplicationException("email exists", ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        @GetMapping("/application/unauthorized")
        void applicationUnauthorized() {

            throw new ApplicationException("bad credentials", ErrorCode.CREDENTIAL_INTEGRITY_ERROR);
        }

        @GetMapping("/application/forbidden")
        void applicationForbidden() {

            throw new ApplicationException("access denied", ErrorCode.ACCESS_DENIED);
        }

        @GetMapping("/application/rate-limited")
        void applicationRateLimited() {

            throw new ApplicationException("rate limit exceeded", ErrorCode.RATE_LIMIT_EXCEEDED);
        }

        @GetMapping("/application/server-error")
        void applicationServerError() {

            throw new ApplicationException("revocation failed", ErrorCode.TOKEN_REVOCATION_FAILED);
        }

        @GetMapping("/account-locked")
        void accountLocked() {

            throw new AccountLockedException(Duration.ofSeconds(30));
        }

        @GetMapping("/infrastructure")
        void infrastructure() {

            throw new InfrastructureException("redis down", new RuntimeException("connection refused"));
        }

        @GetMapping("/unexpected")
        void unexpected() {

            throw new RuntimeException("unexpected");
        }

        record ValidatedBody(@NotBlank String name) {

        }
    }
}
