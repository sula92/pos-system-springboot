package com.idet.pos.aop;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.logging.Logger;

/**
 * Emits lightweight audit logs for state-changing service operations.
 *
 * Why this is useful:
 * - Tracks business mutations (create/update/delete/order placement).
 * - Provides a simple timeline even before introducing a full audit table.
 */
@Aspect
@Component
public class AuditAspect {

    private static final Logger logger = Logger.getLogger(AuditAspect.class.getName());

    /**
     * AfterReturning logs successful write-like operations only.
     * Read-only methods are excluded to keep logs focused and compact.
     */
    @AfterReturning(
            "execution(* com.idet.pos.service..save*(..)) || " +
            "execution(* com.idet.pos.service..update*(..)) || " +
            "execution(* com.idet.pos.service..delete*(..)) || " +
            "execution(* com.idet.pos.service.OrderService.placeOrder(..)) || " +
            "execution(* com.idet.pos.service.InventoryService.createInventory(..))"
    )
    public void logAuditEvent(JoinPoint joinPoint) {
        String method = joinPoint.getSignature().toShortString();
        logger.info("[AOP-AUDIT] " + method + " succeeded at " + Instant.now());
    }
}

