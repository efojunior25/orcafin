package com.orcafin.controller;

import com.orcafin.dto.WhatsappVerifyCodeRequest;
import com.orcafin.entity.PhoneVerificationToken;
import com.orcafin.entity.User;
import com.orcafin.exception.TooManyRequestsException;
import com.orcafin.repository.PhoneVerificationTokenRepository;
import com.orcafin.repository.UserRepository;
import com.orcafin.security.RateLimiterService;
import com.orcafin.service.AiParsingService;
import com.orcafin.service.PendingWhatsappEntryService;
import com.orcafin.service.TransactionService;
import com.orcafin.service.WhisperService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InternalWhatsappControllerTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PhoneVerificationTokenRepository phoneVerificationTokenRepository;
    @Mock
    private PendingWhatsappEntryService pendingWhatsappEntryService;
    @Mock
    private AiParsingService aiParsingService;
    @Mock
    private WhisperService whisperService;
    @Mock
    private TransactionService transactionService;

    private InternalWhatsappController controller;

    @BeforeEach
    void setUp() {
        controller = new InternalWhatsappController(userRepository, phoneVerificationTokenRepository,
                pendingWhatsappEntryService, new RateLimiterService(), aiParsingService, whisperService,
                transactionService);
    }

    @Test
    void blocksPhoneAfterFiveWrongCodes() {
        when(phoneVerificationTokenRepository.findByCode(anyString())).thenReturn(Optional.empty());

        for (int i = 0; i < 5; i++) {
            controller.verifyCode(request("5511999990000", "00000" + i));
        }

        assertThatThrownBy(() -> controller.verifyCode(request("5511999990000", "123456")))
                .isInstanceOf(TooManyRequestsException.class);
    }

    @Test
    void blocksEveryoneAfterThirtyWrongCodesAcrossPhones() {
        when(phoneVerificationTokenRepository.findByCode(anyString())).thenReturn(Optional.empty());

        for (int i = 0; i < 30; i++) {
            controller.verifyCode(request("55119999900" + String.format("%02d", i), "000000"));
        }

        assertThatThrownBy(() -> controller.verifyCode(request("5511888880000", "123456")))
                .isInstanceOf(TooManyRequestsException.class);
        verify(phoneVerificationTokenRepository, never()).findByCode("123456");
    }

    @Test
    void validCodeLinksPhoneAndClearsFailures() {
        User user = new User();
        user.setName("Edson");
        PhoneVerificationToken token = new PhoneVerificationToken();
        token.setUser(user);
        token.setCode("654321");
        token.setExpiresAt(Instant.now().plus(Duration.ofMinutes(10)));
        when(phoneVerificationTokenRepository.findByCode(anyString())).thenReturn(Optional.empty());
        when(phoneVerificationTokenRepository.findByCode("654321")).thenReturn(Optional.of(token));

        for (int i = 0; i < 4; i++) {
            controller.verifyCode(request("5511999990000", "00000" + i));
        }
        var response = controller.verifyCode(request("5511999990000", "654321"));

        assertThat(response.getBody().getMessage()).contains("vinculado");
        assertThat(user.getPhoneNumber()).isEqualTo("5511999990000");
        assertThat(token.isUsed()).isTrue();
    }

    private WhatsappVerifyCodeRequest request(String phone, String code) {
        WhatsappVerifyCodeRequest request = new WhatsappVerifyCodeRequest();
        request.setPhone(phone);
        request.setCode(code);
        return request;
    }
}
