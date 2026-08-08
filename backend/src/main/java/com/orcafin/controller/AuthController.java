package com.orcafin.controller;

import com.orcafin.dto.AuthResponse;
import com.orcafin.dto.ForgotPasswordRequest;
import com.orcafin.dto.LoginAuditResponse;
import com.orcafin.dto.LoginRequest;
import com.orcafin.dto.RegisterRequest;
import com.orcafin.dto.ResetPasswordRequest;
import com.orcafin.entity.LoginAudit;
import com.orcafin.entity.PasswordResetToken;
import com.orcafin.entity.User;
import com.orcafin.repository.LoginAuditRepository;
import com.orcafin.repository.PasswordResetTokenRepository;
import com.orcafin.repository.UserRepository;
import com.orcafin.security.JwtService;
import com.orcafin.security.RateLimiterService;
import com.orcafin.security.SecurityUtils;
import com.orcafin.service.EmailService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final int LOGIN_MAX_ATTEMPTS = 10;
    private static final Duration LOGIN_WINDOW = Duration.ofMinutes(5);
    private static final int REGISTER_MAX_ATTEMPTS = 10;
    private static final Duration REGISTER_WINDOW = Duration.ofMinutes(5);
    private static final int FORGOT_PASSWORD_MAX_ATTEMPTS = 5;
    private static final Duration FORGOT_PASSWORD_WINDOW = Duration.ofMinutes(15);
    private static final Duration RESET_TOKEN_TTL = Duration.ofHours(1);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final RateLimiterService rateLimiterService;
    private final LoginAuditRepository loginAuditRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailService emailService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request, HttpServletRequest httpRequest) {
        String key = "register:" + clientIp(httpRequest);
        rateLimiterService.checkAllowed(key, REGISTER_MAX_ATTEMPTS, REGISTER_WINDOW,
                "Muitas contas criadas a partir deste endereço. Tente novamente mais tarde.");
        rateLimiterService.recordAttempt(key, REGISTER_WINDOW);

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email já cadastrado");
        }

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        userRepository.save(user);

        emailService.sendWelcomeEmail(user.getEmail(), user.getName());

        String token = jwtService.generateToken(user);
        return ResponseEntity.ok(new AuthResponse(token, user.getName(), user.getEmail()));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request, HttpServletRequest httpRequest) {
        String key = "forgot-password:" + clientIp(httpRequest);
        rateLimiterService.checkAllowed(key, FORGOT_PASSWORD_MAX_ATTEMPTS, FORGOT_PASSWORD_WINDOW,
                "Muitas solicitações. Tente novamente mais tarde.");
        rateLimiterService.recordAttempt(key, FORGOT_PASSWORD_WINDOW);

        userRepository.findByEmail(request.getEmail()).ifPresent(user -> {
            PasswordResetToken resetToken = new PasswordResetToken();
            resetToken.setUser(user);
            resetToken.setToken(UUID.randomUUID().toString());
            resetToken.setExpiresAt(Instant.now().plus(RESET_TOKEN_TTL));
            passwordResetTokenRepository.save(resetToken);
            emailService.sendPasswordResetEmail(user.getEmail(), user.getName(), resetToken.getToken());
        });

        // Mesma resposta exista ou não o email, pra não revelar quais emails estão cadastrados.
        return ResponseEntity.ok(Map.of("message", "Se esse email existir, enviamos um link de redefinição."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> new IllegalArgumentException("Link de redefinição inválido ou expirado."));

        if (resetToken.isUsed() || Instant.now().isAfter(resetToken.getExpiresAt())) {
            throw new IllegalArgumentException("Link de redefinição inválido ou expirado.");
        }

        User user = resetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);

        return ResponseEntity.ok(Map.of("message", "Senha redefinida com sucesso."));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        String ip = clientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        String key = "login:" + ip;
        rateLimiterService.checkAllowed(key, LOGIN_MAX_ATTEMPTS, LOGIN_WINDOW,
                "Muitas tentativas de login. Tente novamente em alguns minutos.");

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
        } catch (BadCredentialsException e) {
            rateLimiterService.recordAttempt(key, LOGIN_WINDOW);
            recordAudit(request.getEmail(), ip, userAgent, false);
            throw e;
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Credenciais inválidas"));

        rateLimiterService.reset(key);
        recordAudit(request.getEmail(), ip, userAgent, true);
        String token = jwtService.generateToken(user);
        return ResponseEntity.ok(new AuthResponse(token, user.getName(), user.getEmail()));
    }

    @GetMapping("/login-history")
    public ResponseEntity<List<LoginAuditResponse>> loginHistory() {
        User user = SecurityUtils.getCurrentUser();
        List<LoginAuditResponse> history = loginAuditRepository
                .findByEmailOrderByCreatedAtDesc(user.getEmail(), PageRequest.of(0, 50))
                .stream()
                .map(LoginAuditResponse::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(history);
    }

    private void recordAudit(String email, String ip, String userAgent, boolean success) {
        LoginAudit audit = new LoginAudit();
        audit.setEmail(email);
        audit.setIp(ip);
        audit.setUserAgent(userAgent);
        audit.setSuccess(success);
        loginAuditRepository.save(audit);
    }

    private String clientIp(HttpServletRequest request) {
        // Cloudflare sempre manda o IP real do visitante aqui, mesmo atravessando o túnel.
        String cfConnectingIp = request.getHeader("CF-Connecting-IP");
        if (cfConnectingIp != null && !cfConnectingIp.isBlank()) {
            return cfConnectingIp.trim();
        }
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
