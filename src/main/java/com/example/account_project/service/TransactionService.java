package com.example.account_project.service;

import com.example.account_project.Exception.AccountException;
import com.example.account_project.domain.Account;
import com.example.account_project.domain.AccountUser;
import com.example.account_project.domain.Transaction;
import com.example.account_project.dto.TransactionDto;
import com.example.account_project.repository.AccountRepository;
import com.example.account_project.repository.AccountUserRepository;
import com.example.account_project.repository.TransactionRepository;
import com.example.account_project.type.AccountStatus;
import com.example.account_project.type.ErrorCode;
import com.example.account_project.type.TransactionResultType;
import com.example.account_project.type.TransactionType;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

import static com.example.account_project.type.TransactionResultType.*;
import static com.example.account_project.type.TransactionType.*;

@Slf4j
@Service
@RequiredArgsConstructor

public class TransactionService {
    private final TransactionRepository transactionRepository;
    private final AccountUserRepository accountUserRepository;
    private final AccountRepository accountRepository;
    /*  ============ 실패 응답 validateUseBalance 메소드 ===========
     * 1. 사용자가 없는 경우
     * 2. 계좌가 없는 경우
     * 3. 사용자 아이디와 계좌 소유주가 다른 경우
     * 4. 계좌가 이미 해지 상태인 경우
     * 5. 거래금액이 잔액보다 큰 경우
     * 6. 거래금액이 너무 작거나 큰 경우*/

    @Transactional
    public TransactionDto useBalance(Long userId, String accountNumber, Long amount) {
        AccountUser user = accountUserRepository.findById(userId)
                .orElseThrow(() -> new AccountException(ErrorCode.USER_NOT_FOUND));
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountException(ErrorCode.ACCOUNT_NOT_FOUND));
        validateUseBalance(user, account, amount);

        account.useBalance(amount);

        return TransactionDto.fromEntity(saveAndGetTransaction(USE, SUCCESS, account, amount));
    }

    private void validateUseBalance(AccountUser user, Account account, Long amount) {
        if (!Objects.equals(user.getId(), account.getAccountUser().getId())) {
            throw new AccountException(ErrorCode.USER_ACCOUNT_UN_MATCH);
        }
        if (account.getAccountStatus() != AccountStatus.IN_USE) {
            throw new AccountException(ErrorCode.ACCOUNT_ALREADY_UNREGISTERED);
        }
        if (account.getBalance() < amount) {
            throw new AccountException(ErrorCode.AMOUNT_EXCEED_BALANCE);
        }
    }

    @Transactional
    public void saveFailedUseTransaction(String accountNumber, Long amount) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountException(ErrorCode.ACCOUNT_NOT_FOUND));

        saveAndGetTransaction(USE, FAIL, account, amount);
    }

    private Transaction saveAndGetTransaction(TransactionType transactionType, TransactionResultType transactionResultType, Account account, Long amount) {


        return transactionRepository.save(
                Transaction.builder()
                        .transactionType(transactionType)
                        .transactionResultType(transactionResultType)
                        .account(account)
                        .amount(amount)
                        .balanceSnapshot(account.getBalance())
                        .transactionId(UUID.randomUUID().toString().replace("-", ""))
                        .transactedAt(LocalDateTime.now())
                        .build()
        );
    }

    public TransactionDto cancelBalance(String transactionId, String accountNumber, Long amount) {
        Transaction transaction = transactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new AccountException(ErrorCode.TRANSACTION_NOT_FOUND));

        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountException(ErrorCode.ACCOUNT_NOT_FOUND));

        validateCancelBalance(transaction, account, amount);

        account.setBalance(account.getBalance()+amount);
        accountRepository.save(account);

        return TransactionDto.fromEntity(saveAndGetTransaction(CANCEL, SUCCESS, account, amount));
    }

    private void validateCancelBalance(Transaction transaction, Account account, Long amount) {
        if (!Objects.equals(transaction.getAccount().getId(), account.getId())) {
            throw new AccountException(ErrorCode.TRANSACTION_ACCOUNT_UN_MATCH);
        }
        if (!Objects.equals(transaction.getAmount(), amount)) {
            throw new AccountException(ErrorCode.CANCEL_MUST_FULLY);
        }
        //현재시간부터 1년 이전이면
        if (transaction.getTransactedAt().isBefore(LocalDateTime.now().minusYears(1))) {
            throw new AccountException(ErrorCode.TO_OLD_ORDER_TO_CANCEL);
        }
    }

    @Transactional
    public void saveFailedCancelTransaction(String accountNumber, Long amount) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountException(ErrorCode.ACCOUNT_NOT_FOUND));
        saveAndGetTransaction(CANCEL, FAIL, account, amount);
    }

    public TransactionDto queryTransaction(String transactionId) {

        return TransactionDto.fromEntity(transactionRepository.findByTransactionId(transactionId)
                .orElseThrow(()->new AccountException(ErrorCode.TRANSACTION_NOT_FOUND)));
    }
}
