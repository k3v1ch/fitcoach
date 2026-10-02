package ru.sportorg.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.util.UriComponentsBuilder;

@Component
class RegistrationEmailListener {

    private static final Logger logger = LoggerFactory.getLogger(RegistrationEmailListener.class);

    private final JavaMailSender mailSender;
    private final String from;
    private final String publicBaseUrl;

    RegistrationEmailListener(JavaMailSender mailSender,
                              @Value("${app.mail.from}") String from,
                              @Value("${app.public-base-url}") String publicBaseUrl) {
        this.mailSender = mailSender;
        this.from = from;
        this.publicBaseUrl = publicBaseUrl;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void sendRegistrationEmail(RegistrationEmailRequested event) {
        send(event.email(), event.token(), "/activate", "Подтверждение регистрации",
            "Чтобы подтвердить адрес и задать пароль, откройте ссылку:");
        }

        @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
        void sendPasswordResetEmail(PasswordResetEmailRequested event) {
        send(event.email(), event.token(), "/reset-password", "Восстановление пароля",
            "Чтобы задать новый пароль, откройте ссылку:");
        }

        private void send(String email, String token, String path, String subject, String introduction) {
        try {
            String activationUrl = UriComponentsBuilder.fromUriString(publicBaseUrl)
                .path(path)
                .queryParam("token", token)
                    .build()
                    .encode()
                    .toUriString();
            var message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(email);
            message.setSubject(subject);
            message.setText(introduction + "\n" + activationUrl
                    + "\nСсылка действует 30 минут и может быть использована один раз.");
            mailSender.send(message);
        } catch (RuntimeException exception) {
            logger.warn("Registration email delivery failed");
        }
    }
}