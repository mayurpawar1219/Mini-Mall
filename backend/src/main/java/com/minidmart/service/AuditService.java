package com.minidmart.service;

import com.minidmart.entity.AuditLog;
import com.minidmart.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Lightweight service for persisting audit log entries.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public void log(UUID actorId, String action, String entityType,
                    String entityId, String details) {
        AuditLog auditLog = AuditLog.builder()
                .actorId(actorId)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .details(details)
                .build();
        auditLogRepository.save(auditLog);
        log.debug("Audit: {} {} {} by {}", action, entityType, entityId, actorId);
    }
}
