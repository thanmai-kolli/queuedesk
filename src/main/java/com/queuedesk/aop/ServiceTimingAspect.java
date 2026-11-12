package com.queuedesk.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * AOP (Aspect-Oriented Programming): code that runs "around" other methods without
 * being written inside them. Logging and timing are "cross-cutting concerns":
 * every service needs them, but none of the services should contain that code.
 *
 * How it works: Spring wraps every bean in com.queuedesk.service in a PROXY object.
 * Controllers talk to the proxy; the proxy runs logServiceCall, which calls the real
 * method with proceed().
 *
 * Note: only calls that go THROUGH the proxy are logged. A service calling one of its
 * own methods (this.someMethod()) skips the proxy, and static methods are never proxied.
 */
@Aspect
@Component
public class ServiceTimingAspect {

    private static final Logger log = LoggerFactory.getLogger(ServiceTimingAspect.class);

    // The pointcut: every public method of every class in the service package (and below).
    @Around("execution(public * com.queuedesk.service..*(..))")
    public Object logServiceCall(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.nanoTime();
        try {
            return joinPoint.proceed();   // run the real method
        } finally {
            long millis = (System.nanoTime() - start) / 1_000_000;
            log.info("{} took {} ms", joinPoint.getSignature().toShortString(), millis);
        }
    }
}
