package com.orcafin.controller;

import com.orcafin.dto.AuthResponse;
import com.orcafin.dto.LoginAuditResponse;
import com.orcafin.dto.LoginRequest;
import com.orcafin.dto.RegisterRequest;
import com.orcafin.entity.LoginAudit;
import com.orcafin.entity.User;
import com.orcafin.repository.LoginAuditRepository;
import com.orcafin.repository.UserRepository;
import com.orcafin.security.JwtService;
import com.orcafin.security.LoginRateLimiterService;
import com.orcafin.security.SecurityUtils;
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

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final LoginRateLimiterService loginRateLimiterService;
    private final LoginAuditRepository loginAuditRepository;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email já cadastrado");
        }

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        userRepository.save(user);

        String token = jwtService.generateToken(user);
        return ResponseEntity.ok(new AuthResponse(token, user.getName(), user.getEmail()));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        String ip = clientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        loginRateLimiterService.checkAllowed(ip);

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
        } catch (BadCredentialsException e) {
            loginRateLimiterService.recordFailure(ip);
            recordAudit(request.getEmail(), ip, userAgent, false);
            throw e;
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Credenciais inválidas"));

        loginRateLimiterService.recordSuccess(ip);
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
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
