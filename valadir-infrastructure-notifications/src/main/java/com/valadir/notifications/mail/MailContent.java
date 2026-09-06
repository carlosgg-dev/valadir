package com.valadir.notifications.mail;

public record MailContent(
    String subject,
    String plainText,
    String html) {

}
