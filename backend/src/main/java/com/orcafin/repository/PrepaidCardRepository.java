package com.orcafin.repository;

import com.orcafin.entity.PrepaidCard;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PrepaidCardRepository extends JpaRepository<PrepaidCard, UUID> {
    List<PrepaidCard> findByUserId(UUID userId);
}
