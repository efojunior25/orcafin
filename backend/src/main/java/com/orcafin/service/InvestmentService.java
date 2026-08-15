package com.orcafin.service;

import com.orcafin.dto.InvestmentRequest;
import com.orcafin.dto.InvestmentResponse;
import com.orcafin.entity.Investment;
import com.orcafin.entity.User;
import com.orcafin.exception.ForbiddenException;
import com.orcafin.exception.ResourceNotFoundException;
import com.orcafin.repository.InvestmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InvestmentService {

    private final InvestmentRepository investmentRepository;

    public List<InvestmentResponse> listInvestments(User user) {
        return investmentRepository.findByUserId(user.getId()).stream()
                .map(InvestmentResponse::new)
                .collect(Collectors.toList());
    }

    @Transactional
    public InvestmentResponse createInvestment(User user, InvestmentRequest request) {
        Investment investment = new Investment();
        investment.setUser(user);
        applyRequest(investment, request);
        investmentRepository.save(investment);
        return new InvestmentResponse(investment);
    }

    @Transactional
    public InvestmentResponse updateInvestment(User user, UUID investmentId, InvestmentRequest request) {
        Investment investment = getOwnedInvestment(user, investmentId);
        applyRequest(investment, request);
        investmentRepository.save(investment);
        return new InvestmentResponse(investment);
    }

    @Transactional
    public void deleteInvestment(User user, UUID investmentId) {
        Investment investment = getOwnedInvestment(user, investmentId);
        investmentRepository.delete(investment);
    }

    private void applyRequest(Investment investment, InvestmentRequest request) {
        investment.setName(request.getName());
        investment.setType(request.getType());
        investment.setInvestedAmount(request.getInvestedAmount());
        investment.setCurrentValue(request.getCurrentValue());
        investment.setNotes(request.getNotes());
        investment.setUpdatedAt(Instant.now());
    }

    private Investment getOwnedInvestment(User user, UUID investmentId) {
        Investment investment = investmentRepository.findById(investmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Investimento não encontrado"));
        if (!investment.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("Você não tem acesso a este investimento");
        }
        return investment;
    }
}
