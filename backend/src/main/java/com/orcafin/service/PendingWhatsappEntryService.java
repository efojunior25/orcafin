package com.orcafin.service;

import com.orcafin.dto.ParseTransactionResponse;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Guarda em memória o resultado interpretado de uma mensagem do WhatsApp
 * aguardando confirmação do usuário, por número de telefone. Mesmo padrão
 * de RateLimiterService: suficiente para uma instância única, sem Redis.
 */
@Service
public class PendingWhatsappEntryService {

    private record Entry(ParseTransactionResponse parsed, Instant createdAt) {
    }

    private static final Duration TTL = Duration.ofMinutes(10);

    private final Map<String, Entry> pendingByPhone = new ConcurrentHashMap<>();

    public void put(String phone, ParseTransactionResponse parsed) {
        pendingByPhone.put(phone, new Entry(parsed, Instant.now()));
    }

    public Optional<ParseTransactionResponse> get(String phone) {
        Entry entry = pendingByPhone.get(phone);
        if (entry == null) {
            return Optional.empty();
        }
        if (Instant.now().isAfter(entry.createdAt().plus(TTL))) {
            pendingByPhone.remove(phone);
            return Optional.empty();
        }
        return Optional.of(entry.parsed());
    }

    public void clear(String phone) {
        pendingByPhone.remove(phone);
    }
}
