package com.example.account_project.service;

import com.example.account_project.aop.AccountLockIdInterface;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class LockAopAspect {
    private final LockService lockService;

    @Around("@annotation(com.example.account_project.aop.AccountLock) && args(request)")
    public Object arrounMethod(ProceedingJoinPoint proceedingJoinPoint
            , AccountLockIdInterface request) throws Throwable {
        //lock 취득 시도
        lockService.lock(request.getAccountNumber());
        try {
            return proceedingJoinPoint.proceed();
        } finally {
            //lock 해제
            lockService.unlock(request.getAccountNumber());
        }
    }
}
