package com.cssms.service;

import com.cssms.entity.AuditLog;
import com.cssms.repository.AuditLogRepository;
import com.cssms.util.CurrentUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository repository;

    /** Writes an audit entry in its own transaction; audit failures never break the business operation. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(String username, String action, String entityName, String entityId, String httpMethod,
                    String path, String status, String details) {
        try {
            AuditLog entry = new AuditLog();
            entry.setUsername(username != null ? username : CurrentUser.usernameOrSystem());
            entry.setAction(action);
            entry.setEntityName(entityName);
            entry.setEntityId(entityId);
            entry.setHttpMethod(httpMethod);
            entry.setPath(path != null && path.length() > 300 ? path.substring(0, 300) : path);
            entry.setStatus(status);
            entry.setIpAddress(clientIp());
            entry.setDetails(details != null && details.length() > 1000 ? details.substring(0, 1000) : details);
            repository.save(entry);
        } catch (RuntimeException ex) {
            log.warn("Failed to write audit log: {}", ex.getMessage());
        }
    }

    public void log(String username, String action, String entityName, String entityId, String details) {
        log(username, action, entityName, entityId, null, null, "SUCCESS", details);
    }

    private String clientIp() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            return attrs.getRequest().getRemoteAddr();
        }
        return null;
    }
}
