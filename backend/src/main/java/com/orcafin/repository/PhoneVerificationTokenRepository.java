package com.orcafin.repository;

import com.orcafin.entity.PhoneVerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PhoneVerificationTokenRepository extends JpaRepository<PhoneVerificationToken, UUID> {
    Optional<PhoneVerificationToken> findByCode(String code);
}
