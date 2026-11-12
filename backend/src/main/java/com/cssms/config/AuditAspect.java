package com.cssms.config;

import com.cssms.service.AuditLogService;
import com.cssms.util.CurrentUser;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/** Records every state-changing API call (POST/PUT/PATCH/DELETE) in audit_logs. */
@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private final AuditLogService auditLogService;

    @Around("within(com.cssms.controller..*) && !within(com.cssms.controller.AuthController) && ("
            + "@annotation(org.springframework.web.bind.annotation.PostMapping) || "
            + "@annotation(org.springframework.web.bind.annotation.PutMapping) || "
            + "@annotation(org.springframework.web.bind.annotation.PatchMapping) || "
            + "@annotation(org.springframework.web.bind.annotation.DeleteMapping))")
    public Object audit(ProceedingJoinPoint joinPoint) throws Throwable {
        HttpServletRequest request = currentRequest();
        String entity = joinPoint.getTarget().getClass().getSimpleName().replace("Controller", "");
        String action = joinPoint.getSignature().getName();
        try {
            Object result = joinPoint.proceed();
            write(request, entity, action, "SUCCESS", null);
            return result;
        } catch (Throwable ex) {
            write(request, entity, action, "FAILED", ex.getClass().getSimpleName() + ": " + ex.getMessage());
            throw ex;
        }
    }

    private void write(HttpServletRequest request, String entity, String action, String status, String details) {
        String method = request != null ? request.getMethod() : null;
        String path = request != null ? request.getRequestURI() : null;
        auditLogService.log(CurrentUser.usernameOrSystem(), action.toUpperCase(), entity, entityId(path), method,
                path, status, details);
    }

    private String entityId(String path) {
        if (path == null) {
            return null;
        }
        for (String segment : path.split("/")) {
            if (segment.matches("\\d+")) {
                return segment;
            }
        }
        return null;
    }

    private HttpServletRequest currentRequest() {
        return RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs
                ? attrs.getRequest() : null;
    }
}
