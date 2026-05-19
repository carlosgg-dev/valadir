package com.valadir.web.config;

public final class ApiRoutes {

    public static final String API = "/api";

    public static final class Auth {

        public static final String BASE = API + "/auth";

        // Session
        public static final String REGISTER = "/register";
        public static final String LOGIN = "/login";
        public static final String REFRESH = "/refresh";
        public static final String LOGOUT = "/logout";

        public static final String REGISTER_PATH = BASE + REGISTER;
        public static final String LOGIN_PATH = BASE + LOGIN;
        public static final String REFRESH_PATH = BASE + REFRESH;
        public static final String LOGOUT_PATH = BASE + LOGOUT;

        // Account activation
        public static final class AccountActivation {

            public static final String ACTIVATE = "/account-activation";
            public static final String RESEND = ACTIVATE + "/resend";

            public static final String ACTIVATE_PATH = Auth.BASE + ACTIVATE;
            public static final String RESEND_PATH = Auth.BASE + RESEND;

            private AccountActivation() {

            }
        }

        // Password reset
        public static final class PasswordReset {

            private static final String RESET = "/password-reset";
            public static final String INITIATE = RESET + "/initiate";
            public static final String VERIFY = RESET + "/verify";
            public static final String COMPLETE = RESET + "/complete";

            public static final String INITIATE_PATH = Auth.BASE + INITIATE;
            public static final String VERIFY_PATH = Auth.BASE + VERIFY;
            public static final String COMPLETE_PATH = Auth.BASE + COMPLETE;

            private PasswordReset() {

            }
        }

        private Auth() {

        }
    }

    private ApiRoutes() {

    }
}
