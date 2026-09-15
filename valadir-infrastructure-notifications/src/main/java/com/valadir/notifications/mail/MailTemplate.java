package com.valadir.notifications.mail;

public enum MailTemplate {

    ACCOUNT_ACTIVATION("account-activation"),
    PASSWORD_RESET("password-reset"),
    ACCOUNT_LOCKED("account-locked"),
    PASSWORD_CHANGED("password-changed"),
    EMAIL_CHANGE("email-change"),
    EMAIL_CHANGED("email-changed");

    private final String baseName;

    MailTemplate(String baseName) {

        this.baseName = baseName;
    }

    String subjectKey() {

        return "mail." + baseName + ".subject";
    }

    String htmlTemplate() {

        return baseName + ".html";
    }

    String textTemplate() {

        return baseName + ".txt";
    }
}
