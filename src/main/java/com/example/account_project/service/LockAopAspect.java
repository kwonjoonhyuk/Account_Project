package com.example.account_project.service;

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

    @Around("@annotation(com.example.account_project.aop.AccountLock)")
    public Object arrounMethod( ProceedingJoinPoint proceedingJoinPoint) throws Throwable {

        //lock 취득 시도
       try {

           return proceedingJoinPoint.proceed();
       }finally {
           //lock 해제
       }
    }
}
