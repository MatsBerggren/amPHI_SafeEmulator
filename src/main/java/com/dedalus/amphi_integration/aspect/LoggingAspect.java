package com.dedalus.amphi_integration.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
public class LoggingAspect {

    @AfterReturning(pointcut = "execution(* com.dedalus.amphi_integration.controller.EvamController.*(..))", returning = "returnValue")
    public void logAfterEvamReturning(JoinPoint joinPoint, Object returnValue) {
        log.debug("Method {} returned: {}", joinPoint.getSignature().toShortString(), returnValue);
    }

    @AfterReturning(pointcut = "execution(* com.dedalus.amphi_integration.controller.AmphiController.*(..))", returning = "returnValue")
    public void logAfterAmphiReturning(JoinPoint joinPoint, Object returnValue) {
        log.debug("Method {} returned: {}", joinPoint.getSignature().toShortString(), returnValue);
    }
}
