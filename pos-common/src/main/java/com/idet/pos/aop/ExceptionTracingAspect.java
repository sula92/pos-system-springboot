package com.idet.pos.aop;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Logs exception context from service/repository calls.
 *
 * Why this is useful:
 * - Keeps failure diagnostics centralized and consistent.
 * - Complements GlobalExceptionHandler with deeper method-level context.
 */
@Aspect
@Component
public class ExceptionTracingAspect {

    private static final Logger logger = Logger.getLogger(ExceptionTracingAspect.class.getName());

    /**
     * AfterThrowing advice runs only when an exception escapes the target method.
     * It captures method signature + argument types + exception type/message.
     * Argument values are not logged: they can hold customer data (name, address, email).
     */
    @AfterThrowing(
            pointcut = "execution(* com.idet.pos.service..*(..)) || execution(* com.idet.pos.repository..*(..))",
            throwing = "exception"
    )
    public void logExceptionContext(JoinPoint joinPoint, Throwable exception) {
        String method = joinPoint.getSignature().toShortString();
        String argTypes = Arrays.stream(joinPoint.getArgs())
                .map(arg -> arg == null ? "null" : arg.getClass().getSimpleName())
                .collect(Collectors.joining(", ", "[", "]"));

        logger.severe("[AOP-EX] Method failed: " + method +
                " | argTypes=" + argTypes +
                " | type=" + exception.getClass().getSimpleName() +
                " | message=" + exception.getMessage());
    }
}

