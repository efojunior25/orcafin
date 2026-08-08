package com.orcafin.controller;

import com.orcafin.dto.DefaultAccountRequest;
import com.orcafin.dto.PhoneCodeResponse;
import com.orcafin.dto.ProfileResponse;
import com.orcafin.entity.Account;
import com.orcafin.entity.PhoneVerificationToken;
import com.orcafin.entity.User;
import com.orcafin.exception.ForbiddenException;
import com.orcafin.exception.ResourceNotFoundException;
import com.orcafin.repository.AccountRepository;
import com.orcafin.repository.PhoneVerificationTokenRepository;
import com.orcafin.repository.UserRepository;
import com.orcafin.security.RateLimiterService;
import com.orcafin.security.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private static final int CODE_REQUEST_MAX_ATTEMPTS = 5;
    private static final Duration CODE_REQUEST_WINDOW = Duration.ofMinutes(15);
    private static final Duration CODE_TTL = Duration.ofMinutes(15);

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final PhoneVerificationTokenRepository phoneVerificationTokenRepository;
    private final RateLimiterService rateLimiterService;
    private final SecureRandom secureRandom = new SecureRandom();

    @GetMapping
    public ResponseEntity<ProfileResponse> getProfile() {
        User user = SecurityUtils.getCurrentUser();
        return ResponseEntity.ok(new ProfileResponse(
                user.getPhoneNumber() != null, user.getPhoneNumber(), user.getDefaultAccountId()));
    }

    @PostMapping("/phone/request-code")
    public ResponseEntity<PhoneCodeResponse> requestPhoneCode(HttpServletRequest httpRequest) {
        User user = SecurityUtils.getCurrentUser();
        String key = "phone-code:" + user.getId();
        rateLimiterService.checkAllowed(key, CODE_REQUEST_MAX_ATTEMPTS, CODE_REQUEST_WINDOW,
                "Muitos pedidos de código. Tente novamente mais tarde.");
        rateLimiterService.recordAttempt(key, CODE_REQUEST_WINDOW);

        String code = generateUniqueCode();
        PhoneVerificationToken token = new PhoneVerificationToken();
        token.setUser(user);
        token.setCode(code);
        token.setExpiresAt(Instant.now().plus(CODE_TTL));
        phoneVerificationTokenRepository.save(token);

        return ResponseEntity.ok(new PhoneCodeResponse(code, (int) CODE_TTL.toMinutes()));
    }

    @PutMapping("/default-account")
    public ResponseEntity<ProfileResponse> setDefaultAccount(@Valid @RequestBody DefaultAccountRequest request) {
        User user = SecurityUtils.getCurrentUser();
        Account account = accountRepository.findById(request.getAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("Conta não encontrada"));
        if (!account.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("Você não tem acesso a esta conta");
        }
        user.setDefaultAccountId(account.getId());
        userRepository.save(user);

        return ResponseEntity.ok(new ProfileResponse(
                user.getPhoneNumber() != null, user.getPhoneNumber(), user.getDefaultAccountId()));
    }

    private String generateUniqueCode() {
        for (int i = 0; i < 20; i++) {
            String code = String.format("%06d", secureRandom.nextInt(1_000_000));
            if (phoneVerificationTokenRepository.findByCode(code).isEmpty()) {
                return code;
            }
        }
        throw new IllegalStateException("Não foi possível gerar um código único");
    }
}
