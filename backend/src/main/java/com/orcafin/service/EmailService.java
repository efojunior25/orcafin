package com.orcafin.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    @Value("${app.mail-from-name}")
    private String fromName;

    @Value("${app.public-url}")
    private String publicUrl;

    public void sendWelcomeEmail(String toEmail, String name) {
        String html = """
                <div style="font-family: sans-serif; max-width: 480px; margin: 0 auto;">
                    <h2>Bem-vindo(a) ao OrçaFin, %s!</h2>
                    <p>Sua conta foi criada com sucesso. Agora você já pode organizar suas finanças por lá.</p>
                    <p><a href="%s" style="background:#22c55e;color:#fff;padding:10px 18px;border-radius:6px;text-decoration:none;">Acessar o OrçaFin</a></p>
                </div>
                """.formatted(name, publicUrl);
        send(toEmail, "Bem-vindo(a) ao OrçaFin", html);
    }

    public void sendPasswordResetEmail(String toEmail, String name, String token) {
        String resetLink = publicUrl + "/reset-password?token=" + token;
        String html = """
                <div style="font-family: sans-serif; max-width: 480px; margin: 0 auto;">
                    <h2>Redefinir senha</h2>
                    <p>Olá, %s. Recebemos um pedido para redefinir a senha da sua conta no OrçaFin.</p>
                    <p><a href="%s" style="background:#3b82f6;color:#fff;padding:10px 18px;border-radius:6px;text-decoration:none;">Redefinir senha</a></p>
                    <p>Esse link expira em 1 hora. Se você não pediu isso, pode ignorar este e-mail.</p>
                </div>
                """.formatted(name, resetLink);
        send(toEmail, "Redefinir sua senha do OrçaFin", html);
    }

    private void send(String toEmail, String subject, String html) {
        if (fromEmail == null || fromEmail.isBlank()) {
            log.warn("MAIL_USERNAME não configurado — email para {} não foi enviado (assunto: {})", toEmail, subject);
            return;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);
        } catch (MailException | java.io.UnsupportedEncodingException | jakarta.mail.MessagingException e) {
            log.error("Falha ao enviar email para {}: {}", toEmail, e.getMessage());
        }
    }
}
