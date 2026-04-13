package com.valadir.web.adapter;

import com.valadir.common.error.ErrorCode;
import com.valadir.web.dto.response.ErrorResponse;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class GlobalErrorController implements ErrorController {

    private static final Logger log = LoggerFactory.getLogger(GlobalErrorController.class);

    @RequestMapping("/error")
    ResponseEntity<ErrorResponse> handleError(HttpServletRequest request) {

        Object statusAttr = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        int statusCode = statusAttr instanceof Integer code ? code : HttpStatus.INTERNAL_SERVER_ERROR.value();
        HttpStatus resolved = HttpStatus.resolve(statusCode);
        HttpStatus status = resolved != null ? resolved : HttpStatus.INTERNAL_SERVER_ERROR;
        Throwable cause = (Throwable) request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);

        if (status.is5xxServerError()) {
            log.error("Unhandled filter-level error: status={}", statusCode, cause);
        } else {
            log.warn("Unhandled filter-level error: status={}", statusCode, cause);
        }

        return ResponseEntity
            .status(status)
            .body(new ErrorResponse(ErrorCode.INTERNAL_SERVER_ERROR.getCode()));
    }
}
