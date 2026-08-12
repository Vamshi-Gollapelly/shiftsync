package com.shiftsync.audit;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

/**
 * Writes audit trail entries. REQUIRES_NEW so an audit write is never rolled
 * back just because the surrounding business transaction later fails for an
 * unrelated reason — the audit log and the "did this action succeed" outcome
 * are treated as independent.
 */
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    // NOTE: Using REQUIRED (default) rather than REQUIRES_NEW for now — audit
    // writes need to see uncommitted rows from the SAME transaction (e.g. a
    // business created seconds earlier in registerBusiness()). A later
    // milestone will revisit true "audit survives rollback" behavior using
    // @TransactionalEventListener(phase = AFTER_COMMIT) instead.
    @Transactional
    public void log(UUID businessId, UUID actorUserId, String action, String entityType, UUID entityId, Map<String, Object> details) {
        AuditLog entry = AuditLog.builder()
                .businessId(businessId)
                .actorUserId(actorUserId)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .details(details)
                .build();
        auditLogRepository.save(entry);
    }
}