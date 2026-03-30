package it.cinofilo.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.base-url}")
    private String baseUrl;

    @Value("${app.mail.from:noreply@centrocinofilo.it}")
    private String fromAddress;

    public void sendVerificationEmail(String to, String token) {
        String link = baseUrl + "/verify-email?token=" + token;
        sendSimple(to,
            "Verifica la tua email - Centro Cinofilo",
            "Benvenuto in Centro Cinofilo!\n\n" +
            "Clicca sul link seguente per verificare la tua email (valido 24 ore):\n\n" +
            link + "\n\n" +
            "Se non hai creato un account, ignora questa email.");
    }

    public void sendPasswordResetEmail(String to, String token) {
        String link = baseUrl + "/reset-password?token=" + token;
        sendSimple(to,
            "Reimposta la tua password - Centro Cinofilo",
            "Hai richiesto il reset della password.\n\n" +
            "Clicca sul link seguente per reimpostare la password (valido 1 ora):\n\n" +
            link + "\n\n" +
            "Se non hai richiesto il reset, ignora questa email.");
    }

    private void sendSimple(String to, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            mailSender.send(message);
            log.info("Email inviata a {}: {}", to, subject);
        } catch (Exception e) {
            // Non propagare: un errore SMTP non deve restituire 500 all'utente.
            // Il token è già salvato nel DB; l'utente può richiedere il reinvio.
            log.error("Impossibile inviare email a {}: {}", to, e.getMessage(), e);
        }
    }
}
