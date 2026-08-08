package com.orcafin.controller;

import com.orcafin.dto.ParseTransactionResponse;
import com.orcafin.dto.TransactionRequest;
import com.orcafin.dto.WhatsappBotResponse;
import com.orcafin.dto.WhatsappConfirmRequest;
import com.orcafin.dto.WhatsappMessageRequest;
import com.orcafin.dto.WhatsappVerifyCodeRequest;
import com.orcafin.entity.PaymentMethod;
import com.orcafin.entity.PhoneVerificationToken;
import com.orcafin.entity.TransactionType;
import com.orcafin.entity.User;
import com.orcafin.repository.PhoneVerificationTokenRepository;
import com.orcafin.repository.UserRepository;
import com.orcafin.security.RateLimiterService;
import com.orcafin.service.AiParsingService;
import com.orcafin.service.PendingWhatsappEntryService;
import com.orcafin.service.TransactionService;
import com.orcafin.service.WhisperService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

/**
 * Rotas chamadas pelo serviço whatsapp-bot, não por usuários logados no app.
 * Autorização feita pelo InternalApiKeyFilter (header X-Internal-Api-Key),
 * não pelo JWT normal — ver SecurityConfig.
 */
@RestController
@RequestMapping("/api/internal/whatsapp")
@RequiredArgsConstructor
public class InternalWhatsappController {

    private static final int MESSAGE_MAX_ATTEMPTS = 30;
    private static final Duration MESSAGE_WINDOW = Duration.ofHours(1);

    private final UserRepository userRepository;
    private final PhoneVerificationTokenRepository phoneVerificationTokenRepository;
    private final PendingWhatsappEntryService pendingWhatsappEntryService;
    private final RateLimiterService rateLimiterService;
    private final AiParsingService aiParsingService;
    private final WhisperService whisperService;
    private final TransactionService transactionService;

    @PostMapping("/verify-code")
    public ResponseEntity<WhatsappBotResponse> verifyCode(@Valid @RequestBody WhatsappVerifyCodeRequest request) {
        Optional<PhoneVerificationToken> tokenOpt = phoneVerificationTokenRepository.findByCode(request.getCode().trim());
        if (tokenOpt.isEmpty()) {
            return ResponseEntity.ok(new WhatsappBotResponse("Código inválido. Gere um novo no app (Perfil > Vincular WhatsApp)."));
        }
        PhoneVerificationToken token = tokenOpt.get();
        if (token.isUsed() || Instant.now().isAfter(token.getExpiresAt())) {
            return ResponseEntity.ok(new WhatsappBotResponse("Esse código expirou. Gere um novo no app."));
        }

        User user = token.getUser();
        user.setPhoneNumber(request.getPhone());
        try {
            userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.ok(new WhatsappBotResponse(
                    "Esse número já está vinculado a outra conta do OrçaFin."));
        }
        token.setUsed(true);
        phoneVerificationTokenRepository.save(token);

        return ResponseEntity.ok(new WhatsappBotResponse(
                "Número vinculado à conta de " + user.getName() + "! Já pode mandar seus lançamentos por aqui."));
    }

    @PostMapping("/message")
    public ResponseEntity<WhatsappBotResponse> message(@Valid @RequestBody WhatsappMessageRequest request) {
        User user = userRepository.findByPhoneNumber(request.getPhone()).orElse(null);
        if (user == null) {
            return ResponseEntity.ok(new WhatsappBotResponse(
                    "Esse número ainda não está vinculado a nenhuma conta do OrçaFin. " +
                            "Abra o app, vá em Perfil e gere um código de vinculação."));
        }

        String key = "whatsapp-message:" + request.getPhone();
        rateLimiterService.checkAllowed(key, MESSAGE_MAX_ATTEMPTS, MESSAGE_WINDOW,
                "Muitas mensagens em pouco tempo. Tente novamente mais tarde.");
        rateLimiterService.recordAttempt(key, MESSAGE_WINDOW);

        ParseTransactionResponse parsed;
        if ("AUDIO".equalsIgnoreCase(request.getType())) {
            byte[] audioBytes = Base64.getDecoder().decode(request.getAudioBase64());
            String transcribedText = whisperService.transcribe(audioBytes);
            parsed = aiParsingService.parse(user, transcribedText, transcribedText);
        } else {
            parsed = aiParsingService.parse(user, request.getText());
        }

        pendingWhatsappEntryService.put(request.getPhone(), parsed);

        String emoji = parsed.getType() == TransactionType.RECEITA ? "💰" : "💸";
        String summary = String.format(
                "Entendi: %s %s de R$ %s em \"%s\"%s.\nResponde *1* pra confirmar ou *2* pra cancelar.",
                emoji,
                parsed.getType() == TransactionType.RECEITA ? "Receita" : "Despesa",
                parsed.getAmount().toPlainString(),
                parsed.getDescription(),
                parsed.getCategoryName() != null ? ", categoria " + parsed.getCategoryName() : "");

        return ResponseEntity.ok(new WhatsappBotResponse(summary));
    }

    @PostMapping("/confirm")
    public ResponseEntity<WhatsappBotResponse> confirm(@Valid @RequestBody WhatsappConfirmRequest request) {
        User user = userRepository.findByPhoneNumber(request.getPhone()).orElse(null);
        if (user == null) {
            return ResponseEntity.ok(new WhatsappBotResponse("Número não vinculado."));
        }

        Optional<ParseTransactionResponse> pendingOpt = pendingWhatsappEntryService.get(request.getPhone());
        if (pendingOpt.isEmpty()) {
            return ResponseEntity.ok(new WhatsappBotResponse(
                    "Não tem nenhum lançamento pendente de confirmação. Manda o texto ou áudio de novo."));
        }

        if (!request.isConfirm()) {
            pendingWhatsappEntryService.clear(request.getPhone());
            return ResponseEntity.ok(new WhatsappBotResponse("Cancelado, nada foi salvo."));
        }

        if (user.getDefaultAccountId() == null) {
            return ResponseEntity.ok(new WhatsappBotResponse(
                    "Você ainda não configurou uma conta padrão pro WhatsApp. Abra o app, vá em Perfil e escolha uma."));
        }

        ParseTransactionResponse parsed = pendingOpt.get();
        TransactionRequest transactionRequest = new TransactionRequest();
        transactionRequest.setAccountId(user.getDefaultAccountId());
        transactionRequest.setCategoryId(parsed.getCategoryId());
        transactionRequest.setType(parsed.getType());
        transactionRequest.setAmount(parsed.getAmount());
        transactionRequest.setDescription(parsed.getDescription());
        transactionRequest.setDate(parsed.getDate());
        transactionRequest.setPaymentMethod(PaymentMethod.DINHEIRO);

        transactionService.createTransaction(user, transactionRequest);
        pendingWhatsappEntryService.clear(request.getPhone());

        return ResponseEntity.ok(new WhatsappBotResponse("Lançamento salvo! ✅"));
    }
}
