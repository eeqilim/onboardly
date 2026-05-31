package com.example.authserver.aop;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class LoggingAspect {

    private static final Logger logger = LoggerFactory.getLogger(LoggingAspect.class);

    // Controller Layer
    @Before("execution(* com.example.authserver.controller.*.*(..))")
    public void logBeforeController(JoinPoint joinPoint) {
        logger.info("Controller Layer: Request started: {}", joinPoint.getSignature().getName());
    }

    @AfterReturning(
            pointcut = "execution(* com.example.authserver.controller.*.*(..))",
            returning = "result"
    )
    public void logAfterController(JoinPoint joinPoint, Object result) {
        logger.info("Controller Layer: Request succeeded: {}", joinPoint.getSignature().getName());
    }

    @AfterThrowing(
            pointcut = "execution(* com.example.authserver.controller.*.*(..))",
            throwing = "ex"
    )
    public void logAfterThrowingController(JoinPoint joinPoint, Exception ex) {
        logger.error("Controller Layer: Request failed: {} | Reason: {}",
                joinPoint.getSignature().getName(), ex.getMessage());
    }

    // Service Layer

    @Before("execution(* com.example.authserver.service.*.*(..))")
    public void logBeforeService(JoinPoint joinPoint) {
        logger.info("Service Layer: Method started: {}", joinPoint.getSignature().getName());
    }

    @AfterReturning(
            pointcut = "execution(* com.example.authserver.service.*.*(..))",
            returning = "result"
    )
    public void logAfterService(JoinPoint joinPoint, Object result) {
        logger.info("Service Layer: Method succeeded: {}", joinPoint.getSignature().getName());
    }

    @AfterThrowing(
            pointcut = "execution(* com.example.authserver.service.*.*(..))",
            throwing = "ex"
    )
    public void logAfterThrowingService(JoinPoint joinPoint, Exception ex) {
        logger.error("Service Layer: Method failed: {} | Reason: {}",
                joinPoint.getSignature().getName(), ex.getMessage());
    }

    // repository Layer
    @Around("execution(* com.example.authserver.repository.*.*(..))")
    public Object logAroundDao(ProceedingJoinPoint joinPoint) throws Throwable {
        logger.info("DAO Layer: Database operation started: {}", joinPoint.getSignature().getName());

        long startTime = System.currentTimeMillis();

        try {
            Object result = joinPoint.proceed();
            long endTime = System.currentTimeMillis();
            logger.info("DAO Layer: Database operation succeeded: {} | Time taken: {}ms",
                    joinPoint.getSignature().getName(), endTime - startTime);
            return result;
        } catch (Exception ex) {
            logger.error("DAO Layer: Database operation failed: {} | Reason: {}",
                    joinPoint.getSignature().getName(), ex.getMessage());
            throw ex;
        }
    }
}
