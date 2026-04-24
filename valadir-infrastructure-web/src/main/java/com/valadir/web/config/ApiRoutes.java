package com.valadir.web.config;

public final class ApiRoutes {

    public static final String API = "/api";

    public static final class Auth {

        public static final String BASE = API + "/auth";
        public static final String REGISTER = "/register";
        public static final String LOGIN = "/login";
        public static final String REFRESH = "/refresh";
        public static final String LOGOUT = "/logout";
        public static final String VERIFY_EMAIL = "/verify-email";
        public static final String RESEND_VERIFICATION = "/resend-verification";

        public static final String REGISTER_PATH = BASE + REGISTER;
        public static final String LOGIN_PATH = BASE + LOGIN;
        public static final String REFRESH_PATH = BASE + REFRESH;
        public static final String LOGOUT_PATH = BASE + LOGOUT;
        public static final String VERIFY_EMAIL_PATH = BASE + VERIFY_EMAIL;
        public static final String RESEND_VERIFICATION_PATH = BASE + RESEND_VERIFICATION;

        private Auth() {

        }
    }

    private ApiRoutes() {

    }
}
