package com.orcafin.dto;

import com.orcafin.entity.LoginAudit;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
public class LoginAuditResponse {
    private final UUID id;
    private final String ip;
    private final boolean success;
    private final String userAgent;
    private final Instant createdAt;

    public LoginAuditResponse(LoginAudit audit) {
        this.id = audit.getId();
        this.ip = audit.getIp();
        this.success = audit.isSuccess();
        this.userAgent = audit.getUserAgent();
        this.createdAt = audit.getCreatedAt();
    }
}
