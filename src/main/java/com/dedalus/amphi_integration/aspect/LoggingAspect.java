package com.dedalus.amphi_integration.aspect;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class LoggingAspect {
    /**
     * Logs the return value of any method in EvamController
     * @param joinPoint the joinpoint of the method
     * @param returnValue the return value of the method
     */
    @AfterReturning(pointcut = "execution(* com.dedalus.amphi_integration.controller.EvamController.*(..))", returning = "returnValue")
    public void logAfterEvamReturning(JoinPoint joinPoint, Object returnValue) {
        System.out.println("Method " + joinPoint.getSignature().toShortString() + " has returned " + returnValue);
    }
    
    /**
     * Logs the return value of any method in AmphiController
     * @param joinPoint the joinpoint of the method
     * @param returnValue the return value of the method
     */
    @AfterReturning(pointcut = "execution(* com.dedalus.amphi_integration.controller.AmphiController.*(..))", returning = "returnValue")
    public void logAfterAmphiReturning(JoinPoint joinPoint, Object returnValue) {
        System.out.println("Method " + joinPoint.getSignature().toShortString() + " has returned " + returnValue);
    }
}