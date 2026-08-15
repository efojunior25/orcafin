package com.orcafin.service;

import com.orcafin.dto.PrepaidCardRequest;
import com.orcafin.dto.PrepaidCardResponse;
import com.orcafin.entity.PrepaidCard;
import com.orcafin.entity.PrepaidCardType;
import com.orcafin.entity.User;
import com.orcafin.exception.ForbiddenException;
import com.orcafin.exception.ResourceNotFoundException;
import com.orcafin.repository.PrepaidCardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PrepaidCardService {

    private final PrepaidCardRepository prepaidCardRepository;

    public List<PrepaidCardResponse> listPrepaidCards(User user) {
        return prepaidCardRepository.findByUserId(user.getId()).stream()
                .map(PrepaidCardResponse::new)
                .collect(Collectors.toList());
    }

    @Transactional
    public PrepaidCardResponse createPrepaidCard(User user, PrepaidCardRequest request) {
        validateSubtype(request);
        PrepaidCard prepaidCard = new PrepaidCard();
        prepaidCard.setUser(user);
        prepaidCard.setName(request.getName());
        prepaidCard.setType(request.getType());
        prepaidCard.setSubtype(request.getType() == PrepaidCardType.VALE_TRANSPORTE ? request.getSubtype() : null);
        prepaidCard.setRechargeDay(request.getRechargeDay());
        prepaidCard.setRechargeAmount(request.getRechargeAmount());
        prepaidCardRepository.save(prepaidCard);
        return new PrepaidCardResponse(prepaidCard);
    }

    @Transactional
    public PrepaidCardResponse updatePrepaidCard(User user, UUID prepaidCardId, PrepaidCardRequest request) {
        validateSubtype(request);
        PrepaidCard prepaidCard = getOwnedPrepaidCard(user, prepaidCardId);
        prepaidCard.setName(request.getName());
        prepaidCard.setType(request.getType());
        prepaidCard.setSubtype(request.getType() == PrepaidCardType.VALE_TRANSPORTE ? request.getSubtype() : null);
        prepaidCard.setRechargeDay(request.getRechargeDay());
        prepaidCard.setRechargeAmount(request.getRechargeAmount());
        prepaidCardRepository.save(prepaidCard);
        return new PrepaidCardResponse(prepaidCard);
    }

    @Transactional
    public void deletePrepaidCard(User user, UUID prepaidCardId) {
        PrepaidCard prepaidCard = getOwnedPrepaidCard(user, prepaidCardId);
        prepaidCardRepository.delete(prepaidCard);
    }

    public PrepaidCard getOwnedPrepaidCard(User user, UUID prepaidCardId) {
        PrepaidCard prepaidCard = prepaidCardRepository.findById(prepaidCardId)
                .orElseThrow(() -> new ResourceNotFoundException("Cartão não encontrado"));
        if (!prepaidCard.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("Você não tem acesso a este cartão");
        }
        return prepaidCard;
    }

    private void validateSubtype(PrepaidCardRequest request) {
        if (request.getType() == PrepaidCardType.VALE_TRANSPORTE && request.getSubtype() == null) {
            throw new IllegalArgumentException("Informe a modalidade do vale-transporte");
        }
    }

    public void credit(PrepaidCard prepaidCard, BigDecimal amount) {
        prepaidCard.setBalance(prepaidCard.getBalance().add(amount));
        prepaidCardRepository.save(prepaidCard);
    }

    public void debit(PrepaidCard prepaidCard, BigDecimal amount) {
        prepaidCard.setBalance(prepaidCard.getBalance().subtract(amount));
        prepaidCardRepository.save(prepaidCard);
    }
}
