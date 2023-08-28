package com.example.account_project.service;

import com.example.account_project.Exception.AccountException;
import com.example.account_project.type.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class LockService {
    private final RedissonClient redissonClient;

    public void lock(String accountNumber) {
        RLock lock = redissonClient.getLock("ACLK" + accountNumber); //lock에 쓰는 키로 쓰겠다.
        log.debug("Trying Lock for accountNumber : {}", accountNumber);
        try {
            boolean isLock = lock.tryLock(1, 5, TimeUnit.SECONDS);
            if (!isLock) {
                log.error("================Lock acquisition failed================");
                throw new AccountException(ErrorCode.ACCOUNT_TRANSACTION_LOCK);
            }
        } catch (Exception e) {
            log.error("Redis lock failed");
        }
    }

    private String getLockKey(String accountNumber) {
        return "ACLK" + accountNumber;
    }
}
